"""Small standalone artist editor: Python standard library; Pillow only for PNG tagging.

Keep beside bloom_rules.json and the assets/ folder. Saves only beside itself,
except the PNG the artist explicitly selects and confirms. Never scans textures.
"""
import argparse
import json
import math
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
CONFIG = ROOT / "bloom_rules.json"
INCLUDE = ROOT / "assets/lbc/shaders/include/bloom_rules.glsl"
BEGIN = "// BEGIN ARTIST RULES"
END = "// END ARTIST RULES"
MODES = {"keep": "LBC_CORE_KEEP", "white": "LBC_CORE_WHITE", "halo": "LBC_CORE_HALO"}


def validate(rules):
    if not isinstance(rules, list) or not 1 <= len(rules) <= 64:
        raise ValueError("Нужно от 1 до 64 правил.")
    seen = set()
    for rule in rules:
        if not isinstance(rule, dict) or not all(key in rule for key in ("source", "halo", "strength", "core")):
            raise ValueError("В правиле нужны source, halo, strength и core.")
        for key in ("source", "halo"):
            value = rule[key]
            if not isinstance(value, list) or len(value) != 3 or any(type(x) is not int or not 0 <= x <= 255 for x in value):
                raise ValueError(f"{key}: нужны три целых RGB от 0 до 255.")
        color = tuple(rule["source"])
        if color in seen:
            raise ValueError(f"Цвет {color} встречается дважды.")
        seen.add(color)
        strength = rule["strength"]
        if type(strength) not in (int, float) or not math.isfinite(strength) or not 0 <= strength <= 4:
            raise ValueError("Сила ореола должна быть от 0 до 4.")
        if not isinstance(rule["core"], str) or rule["core"] not in MODES:
            raise ValueError("Сердцевина: keep, white или halo.")
    return rules


def generated(rules):
    lines = [BEGIN, "const LbcBloomRule LBC_BLOOM_RULES[] = LbcBloomRule[]("]
    rgb = lambda value: "ivec3(" + ",".join(map(str, value)) + ")"
    for i, rule in enumerate(validate(rules)):
        strength = f'{float(rule["strength"]):.4f}'.rstrip("0")
        if strength.endswith("."): strength += "0"
        comma = "," if i + 1 < len(rules) else ""
        lines.append(f'    LbcBloomRule({rgb(rule["source"])}, {rgb(rule["halo"])}, {strength}, {MODES[rule["core"]]}){comma}')
    lines.extend([");", END])
    text = INCLUDE.read_text(encoding="utf-8")
    pattern = re.compile(re.escape(BEGIN) + ".*?" + re.escape(END), re.S)
    if len(pattern.findall(text)) != 1:
        raise ValueError("В bloom_rules.glsl отсутствует единственный блок ARTIST RULES.")
    return pattern.sub(lambda match: "\n".join(lines), text)


def save(rules):
    glsl = generated(rules)
    for path, text in ((CONFIG, json.dumps(rules, ensure_ascii=False, indent=2) + "\n"), (INCLUDE, glsl)):
        temporary = path.with_suffix(path.suffix + ".tmp")
        temporary.write_text(text, encoding="utf-8", newline="\n")
        temporary.replace(path)


def mark_png(path, rgb):
    from PIL import Image
    path = Path(path)
    rgb = tuple(rgb)
    validate([{"source": list(rgb), "halo": list(rgb), "strength": 1, "core": "keep"}])
    with Image.open(path) as image:
        if getattr(image, "n_frames", 1) != 1:
            raise ValueError("Анимированный PNG не поддерживается этой кнопкой.")
        if image.width * image.height > 4096 * 4096:
            raise ValueError("Текстура слишком велика для кнопки разметки: предел 4096×4096.")
        rgba = image.convert("RGBA")
    # PixelAccess keeps memory bounded by the image, not millions of Python tuples.
    pixels = rgba.load()
    count = 0
    for y in range(rgba.height):
        for x in range(rgba.width):
            pixel = pixels[x, y]
            if pixel[:3] == rgb and pixel[3] in (254, 255):
                pixels[x, y] = (*rgb, 254)
                count += 1
    if count == 0:
        raise ValueError("Нет непрозрачных пикселей с точным RGB выбранного правила.")
    backup = path.with_suffix(".png.bak")
    if not backup.exists():
        backup.write_bytes(path.read_bytes())
    temporary = path.with_suffix(".png.tmp")
    rgba.save(temporary, format="PNG")
    temporary.replace(path)
    return count


def gui(rules, smoke=False):
    import tkinter as tk
    from tkinter import colorchooser, filedialog, messagebox, ttk
    window = tk.Tk()
    if smoke: window.withdraw()
    window.title("LBC · Свечение материалов")
    window.geometry("770x480")
    window.minsize(700, 430)
    table = ttk.Treeview(window, columns=("source", "halo", "strength", "core"), show="headings", height=12)
    labels = ("RGB в PNG", "Цвет ореола", "Сила 0–4", "Сердцевина")
    for name, label in zip(table["columns"], labels):
        table.heading(name, text=label)
        table.column(name, width=150)
    table.pack(fill="both", expand=True, padx=12, pady=12)
    modes = {"keep": "Исходный цвет", "white": "Белая", "halo": "Цвет ореола"}

    def refresh(selected=None):
        table.delete(*table.get_children())
        for i, rule in enumerate(rules):
            table.insert("", "end", iid=str(i), values=(str(rule["source"]), str(rule["halo"]), rule["strength"], modes[rule["core"]]))
        if selected is not None and selected < len(rules): table.selection_set(str(selected))

    def edit(creating=False):
        selection = table.selection()
        if not creating and not selection: return
        index = None if creating else int(selection[0])
        current = {"source": [255, 3, 3], "halo": [255, 3, 3], "strength": 1.0, "core": "keep"} if creating else dict(rules[index])
        dialog = tk.Toplevel(window)
        dialog.title("Новое правило" if creating else "Изменить правило")
        dialog.transient(window)
        dialog.grab_set()
        color_buttons = {}

        def choose(key):
            initial = "#" + "".join(f"{x:02x}" for x in current[key])
            rgb, _ = colorchooser.askcolor(initial, parent=dialog)
            if rgb is not None:
                current[key] = [round(x) for x in rgb]
                luminance = sum(x * weight for x, weight in zip(current[key], (0.2126, 0.7152, 0.0722)))
                color_buttons[key].configure(text=str(current[key]), bg="#" + "".join(f"{x:02x}" for x in current[key]), fg="white" if luminance < 128 else "black")

        for row, (key, label) in enumerate((("source", "Цвет пикселя в PNG"), ("halo", "Цвет ореола"))):
            ttk.Label(dialog, text=label).grid(row=row, column=0, padx=10, pady=8)
            button = tk.Button(dialog, text=str(current[key]), command=lambda k=key: choose(k))
            button.grid(row=row, column=1, padx=10)
            color_buttons[key] = button
        ttk.Label(dialog, text="Сила: 0 — нет, 1 — обычная, 4 — максимум").grid(row=2, column=0, padx=10)
        strength = tk.StringVar(value=str(current["strength"]))
        ttk.Spinbox(dialog, from_=0, to=4, increment=0.25, textvariable=strength, width=12).grid(row=2, column=1, padx=10, pady=8)
        core = tk.StringVar(value=modes[current["core"]])
        ttk.Label(dialog, text="Сердцевина").grid(row=3, column=0, padx=10)
        ttk.Combobox(dialog, textvariable=core, values=list(modes.values()), state="readonly").grid(row=3, column=1, padx=10, pady=8)

        def accept():
            try:
                current["strength"] = float(strength.get().replace(",", "."))
                current["core"] = next(key for key, label in modes.items() if label == core.get())
                proposed = list(rules)
                if creating: proposed.append(current)
                else: proposed[index] = current
                validate(proposed)
                rules[:] = proposed
                refresh(len(rules) - 1 if creating else index)
                dialog.destroy()
            except (ValueError, KeyError) as error: messagebox.showerror("Проверь правило", str(error), parent=dialog)

        ttk.Button(dialog, text="Применить к списку", command=accept).grid(row=4, column=0, columnspan=2, pady=12)

    def remove():
        selected = table.selection()
        if selected:
            del rules[int(selected[0])]
            refresh()

    def write():
        try:
            save(rules)
            messagebox.showinfo("Готово", "Правила сохранены рядом с редактором.\nСкопируй папку assets в ресурс-пак и нажми F3+T.")
        except (OSError, ValueError, KeyError) as error: messagebox.showerror("Не удалось сохранить", str(error))

    def tag_png():
        selected = table.selection()
        if not selected: return
        try:
            from PIL import Image
        except ImportError:
            messagebox.showerror("Для PNG нужен Pillow", "Правила можно редактировать без него.\nДля разметки PNG: py -m pip install Pillow")
            return
        name = filedialog.askopenfilename(title="Выбери PNG текстуры предмета", filetypes=[("PNG", "*.png")])
        if not name: return
        path = Path(name)
        if not messagebox.askyesno("Разметить текстуру?", f"Пометить RGB {rules[int(selected[0])]['source']} альфой 254?\n{path}\n\nПрозрачные и полупрозрачные пиксели сохранятся. Будет создана резервная копия .png.bak."): return
        try:
            rgb = tuple(rules[int(selected[0])]["source"])
            count = mark_png(path, rgb)
            messagebox.showinfo("PNG размечен", f"Помечено пикселей: {count}.\nОбычные области текстуры не изменены.")
        except (OSError, ValueError) as error: messagebox.showerror("Не удалось разметить PNG", str(error))

    buttons = ttk.Frame(window)
    buttons.pack(fill="x", padx=12)
    for label, action in (("Добавить", lambda: edit(True)), ("Изменить", edit), ("Удалить", remove), ("Разметить PNG…", tag_png), ("Сохранить", write)):
        ttk.Button(buttons, text=label, command=action).pack(side="left", padx=3)
    ttk.Label(window, text="Свечение: RGB из списка + альфа 254 в PNG. Альфа 255 — обычный материал.\nРедактор меняет только выбранный PNG и файлы рядом с собой. Подробности — README_RU.txt.").pack(padx=12, pady=12)
    table.bind("<Double-1>", lambda event: edit())
    refresh()
    if smoke:
        window.update_idletasks()
        assert len(table.get_children()) == len(rules)
        window.destroy()
        print(f"PASS hidden Tk editor: {len(rules)} rows")
    else:
        window.mainloop()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write-glsl", action="store_true", help="Generate GLSL from JSON without opening GUI")
    parser.add_argument("--check", action="store_true", help="Validate JSON and check generated GLSL without writing")
    parser.add_argument("--check-ui", action="store_true", help="Construct and check a hidden editor, then exit without writing")
    args = parser.parse_args()
    rules = validate(json.loads(CONFIG.read_text(encoding="utf-8")))
    if args.check_ui:
        gui(rules, smoke=True)
    elif args.check:
        if INCLUDE.read_text(encoding="utf-8") != generated(rules): raise SystemExit("GLSL отличается от JSON: запусти --write-glsl")
        print(f"PASS {len(rules)} bloom rules")
    elif args.write_glsl:
        save(rules)
        print(f"Generated {len(rules)} bloom rules")
    else:
        gui(rules)


if __name__ == "__main__":
    main()
