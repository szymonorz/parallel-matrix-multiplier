import random
import argparse
import os

def generate_matrix(N):
    matrix = [[random.randint(0, 9) for _ in range(N)] for _ in range(N)]
    return matrix

def write_matrix_to_file(N, matrix, file_path):
    try:
        with open(file_path, 'w') as f:
            f.write(f"{N}\n")
            for row in matrix:
                f.write(" ".join(map(str, row)) + '\n')
        print(f"Matrix successfully written to {file_path}")
    except Exception as e:
        print(f"Error writing to file: {e}")

def concat_matrix_parts(destination):
    dir = os.path.dirname(destination)
    basename = os.path.basename(destination)
    files = [dir + "/" + f for f in os.listdir(dir) if f.startswith(basename + '.part-')]

    if not files:
        print("No parts found for the given destination.")
        return None

    parts = {}
    for file in files:
        try:
            _, indices = file.split('.part-')
            i, j = map(int, indices.split('-'))
            with open(file, 'r') as f:
                value = int(f.read().strip())
            parts[(i, j)] = value
        except ValueError:
            print(f"Skipping invalid file: {file}")

    N = max(i for i, j in parts.keys()) + 1

    matrix = [[0] * N for _ in range(N)]

    for (i, j), value in parts.items():
        matrix[i][j] = value

    write_matrix_to_file(N, matrix, destination)

def main():
    parser = argparse.ArgumentParser(description="Helper script")

    parser.add_argument('--generate', type=str, help="Genearete matrix at path")
    parser.add_argument('--size', type=int, help="Size of generated matrix")
    parser.add_argument('--concat', type=str, default=1, help="Concat matrix results")

    args = parser.parse_args()
    if args.generate:
        N = args.size
        file_path = args.generate
        print(args.generate, args.size)
        matrix = generate_matrix(N)
        write_matrix_to_file(N, matrix, file_path)
    elif args.concat:
        concat_matrix_parts(args.concat)


if __name__ == "__main__":
    main()

