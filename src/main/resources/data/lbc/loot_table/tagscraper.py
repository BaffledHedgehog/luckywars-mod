import os
import json
import re

def extract_key_values_from_tag(tag_str):
    clean_str = tag_str.strip('{}')
    pairs = re.findall(r'(?<!\\)(?:"|\\")?([\w\d_]+)(?:"|\\")?\s*:\s*(true|false|\d+)', clean_str)
    return {f"{key}:{value}" for key, value in pairs}

def search_custom_data(obj):
    results = set()
    if isinstance(obj, dict):
        for key, value in obj.items():
            if key == "minecraft:custom_data" and isinstance(value, str):
                results |= extract_key_values_from_tag(value)
            elif key == "minecraft:custom_data" and isinstance(value, dict):
                for k, v in value.items():
                    results.add(f"{k}:{str(v).lower()}")
            elif key == "function" and value == "minecraft:set_custom_data":
                tag_val = obj.get("tag")
                if isinstance(tag_val, str):
                    results |= extract_key_values_from_tag(tag_val)
            else:
                results |= search_custom_data(value)
    elif isinstance(obj, list):
        for item in obj:
            results |= search_custom_data(item)
    return results

def load_json_file(path):
    try:
        with open(path, encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return None

def find_all_custom_data_entries(root_dir='.'):
    collected_data = set()
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith('.json'):
                full_path = os.path.join(dirpath, filename)
                data = load_json_file(full_path)
                if data:
                    entries = search_custom_data(data)
                    collected_data |= entries
    return sorted(collected_data)

if __name__ == "__main__":
    result = find_all_custom_data_entries()

    with open("custom_data_list.txt", "w", encoding="utf-8") as f:
        for line in result:
            f.write(f"{line}\n")

    print(f"Сохранено {len(result)} записей в custom_data_list.txt")
