"""Read-only witness for the authored lightning centerline and native MC render order."""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--pack', type=Path, required=True)
parser.add_argument('--sources', type=Path, default=Path('.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar'))
parser.add_argument('--output', type=Path, default=Path('build/verification/bot-revision-2026-11-10/static-charge-render-contract.json'))
args = parser.parse_args()
digest = lambda raw: hashlib.sha256(raw).hexdigest()
definition_path = args.pack / 'assets/lbc/items/lightning_bolt_linear.json'
model_path = args.pack / 'assets/lbc/models/item/lightning_bolt_linear.json'
definition = json.loads(definition_path.read_bytes())
model = json.loads(model_path.read_bytes())
assert definition == {'model': {'type': 'model', 'model': 'lbc:item/lightning_bolt_linear'}}
assert 'parent' not in model and 'none' not in model.get('display', {})
assert len(model['elements']) == 2
for element in model['elements']:
    assert element['from'][2] == -8 and element['to'][2] == 8
    assert [(element['from'][i] + element['to'][i]) / 2 for i in (0, 1)] == [8, 8]
    assert element['rotation']['axis'] == 'z' and element['rotation']['origin'] == [8, 8, 0]
contracts = {
    'net/minecraft/client/renderer/entity/DisplayRenderer.java': [
        'case FIXED -> output.rotationYXZ(',
        'poseStack.mulPose(transformation);',
        'poseStack.rotate(Axis.YP, (float) Math.PI);',
    ],
    'net/minecraft/client/resources/model/cuboid/ItemTransform.java': [
        'if (this == NO_TRANSFORM)',
        'pose.translate(-0.5F, -0.5F, -0.5F);',
    ],
    'net/minecraft/client/resources/model/cuboid/FaceBakery.java': [
        'vertexInfo.select(from, to).div(16.0F)',
    ],
}
sources = {}
with ZipFile(args.sources) as archive:
    for name, witnesses in contracts.items():
        raw = archive.read(name)
        source = raw.decode('utf-8')
        assert all(witness in source for witness in witnesses), name
        sources[name] = {'sha256': digest(raw), 'witnesses': witnesses}
result = {
    'passed': True,
    'packReadOnly': True,
    'assets': {str(path): digest(path.read_bytes()) for path in (definition_path, model_path)},
    'nativeSourceContracts': sources,
    'authoredModelCenterline': [[8, 8, 8], [8, 8, -8]],
    'centeredNativeItemAxis': [[0, 0, 0], [0, 0, -1]],
    'afterItemDisplayY180': [[0, 0, 0], [0, 0, 1]],
    'negativeZScaleRequiresImpactAnchor': True,
    'nativeFixture': 'StaticChargeGeometryChecks decodes AddEntity+SetEntityData and checks actual transformed endpoints',
    'limits': ['Source/model transform contract and packet geometry; no physical client GPU frame captured'],
}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'passed': True, 'assets': len(result['assets']), 'nativeSourceContracts': len(sources), 'output': str(args.output)}))
