import random

def generate_matrix(P):
    """Generates a P x P matrix with random integers."""
    matrix = [[random.randint(0, 9) for _ in range(P)] for _ in range(P)]
    return matrix

def write_matrix_to_file(matrix, file_path):
    """Writes the matrix to a file."""
    try:
        with open(file_path, 'w') as f:
            for row in matrix:
                f.write(" ".join(map(str, row)) + '\n')
        print(f"Matrix successfully written to {file_path}")
    except Exception as e:
        print(f"Error writing to file: {e}")

def main():
    # Get the matrix size (P) from the user
    P = int(input("Enter the size of the matrix (P x P): "))

    # Get the file path to write the matrix
    file_path = input("Enter the file path where the matrix should be saved: ")

    # Generate the matrix
    matrix = generate_matrix(P)

    # Write the matrix to the file
    write_matrix_to_file(matrix, file_path)

if __name__ == "__main__":
    main()

