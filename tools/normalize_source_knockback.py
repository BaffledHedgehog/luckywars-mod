"""Normalize authored ordinary loot defaults, never inventories or runtime commands.

Called only once the complete imported loot tree has been schema migrated, so
custom_data on a later function can exempt the entire authored item.
"""
from __future__ import annotations
import re
import math

MAX_COMBINED_KNOCKBACK = 5
#1.21.4 conventional stationary cardinal full hit: .4+.6*6 first exceeds packet3.9.
# Per-axis clipping, resistance and existing motion prevent exact vector equivalence.
MAX_SOURCE_PUNCH = 6
_IMBA = re.compile(r'(?:^|[,{])\s*(?:"imba"|imba)\s*:\s*1(?:[bBsSlL])?\s*(?:[,}]|$)')


def is_imba(value):
    """Read this item's own marker; contained items/entities have independent provenance."""
    if isinstance(value, dict):
        if value.get('type', value.get('function')) == 'minecraft:set_custom_data':
            tag = value.get('tag', '')
            if isinstance(tag, str) and _IMBA.search(tag):
                return True
            if isinstance(tag, dict) and tag.get('imba') == 1:
                return True
        data = value.get('minecraft:custom_data', value.get('custom_data'))
        if isinstance(data, dict) and data.get('imba') == 1:
            return True
        if isinstance(data, str) and _IMBA.search(data):
            return True
        return any(is_imba(value[key]) for key in ('modifier', 'functions', 'components') if key in value)
    if isinstance(value, list):
        return any(is_imba(v) for v in value)
    return False


def normalize_source_knockback(value):
    """Mutate complete ordinary item entries; retain every unrelated field."""
    if isinstance(value, list):
        for child in value:
            normalize_source_knockback(child)
    elif isinstance(value, dict):
        kind = value.get('type', '')
        if kind in ('minecraft:item', 'item'):
            if is_imba(value):
                return
            components = []
            def collect(node):
                if isinstance(node, dict):
                    if node.get('type') == 'minecraft:set_components':
                        components.append(node.get('components', {}))
                    for key in ('modifier', 'functions'):
                        if key in node:
                            collect(node[key])
                elif isinstance(node, list):
                    for child in node:
                        collect(child)
            collect(value)
            # Current ordinary source has additive held attribute contributions.
            # Keep ids/slots/operations, lowering only positive source amounts.
            enchants = [c['minecraft:enchantments'] for c in components
                        if isinstance(c.get('minecraft:enchantments'), dict)]
            for enchant in enchants:
                if enchant.get('minecraft:punch', 0) > MAX_SOURCE_PUNCH:
                    enchant['minecraft:punch'] = MAX_SOURCE_PUNCH
            level = max((e.get('minecraft:knockback', 0) for e in enchants), default=0)
            modifiers = [m for c in components for m in c.get('minecraft:attribute_modifiers', [])
                          if m.get('type') == 'minecraft:attack_knockback'
                          and m.get('slot', 'any') in ('mainhand', 'hand', 'any')]
            if any(m.get('operation') != 'add_value' for m in modifiers):
                raise ValueError('Review non-additive authored attack_knockback before porting')
            positive = [m for m in modifiers if m.get('operation') == 'add_value' and m.get('amount', 0) > 0]
            total = sum(m.get('amount', 0) for m in modifiers if m.get('operation') == 'add_value')
            excess = max(0, total + level - MAX_COMBINED_KNOCKBACK)
            for modifier in positive:
                reduction = min(modifier['amount'], excess)
                modifier['amount'] -= reduction
                excess -= reduction
            if excess > 0:
                for enchant in enchants:
                    if 'minecraft:knockback' in enchant:
                        enchant['minecraft:knockback'] = min(enchant['minecraft:knockback'], max(0, level - math.ceil(excess)))
        for child in value.values():
            normalize_source_knockback(child)
