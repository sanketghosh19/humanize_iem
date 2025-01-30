# Run a Java class from Python Enviornment
import subprocess
import os


def compile_and_run_java_code():
    # Write the Java code to a file
    filename = "TextractPdfGenerator.java"

    libs_path = "javalibs"
    classpath = f".:{libs_path}/*" if os.name != "nt" else f".;{libs_path}\\*"

    # Compile the Java code
    compile_process = subprocess.run(
        ["javac", "-cp", classpath, filename], capture_output=True, text=True
    )

    if compile_process.returncode != 0:
        return f"Compilation Error:\n{compile_process.stderr}"

    # Run the compiled Java code
    run_process = subprocess.run(
        ["java", "-cp", classpath, "TextractPdfGenerator"], capture_output=True, text=True
    )

    return (
        run_process.stdout
        if run_process.returncode == 0
        else f"Runtime Error:\n{run_process.stderr}"
    )


def main():
    output = compile_and_run_java_code()
    print("Output from running Java code:")
    print(output)


if __name__ == "__main__":
    main()
