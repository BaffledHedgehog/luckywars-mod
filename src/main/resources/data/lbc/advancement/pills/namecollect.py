import os

def list_filenames(directory, output_file):
    with open(output_file, 'w', encoding='utf-8') as f:
        for filename in os.listdir(directory):
            if os.path.isfile(os.path.join(directory, filename)):
                name, _ = os.path.splitext(filename)
                f.write(name + '\n')

if __name__ == "__main__":
    directory = "./"  # Укажите нужную папку
    output_file = "file_list.txt"  # Имя выходного файла
    list_filenames(directory, output_file)
    print(f"Список файлов сохранён в {output_file}")
