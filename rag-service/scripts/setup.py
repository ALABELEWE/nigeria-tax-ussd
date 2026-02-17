"""
Complete setup script for Nigerian Tax RAG Service
Installs Python dependencies and pulls Ollama models
"""
import subprocess
import sys
from pathlib import Path

def run_command(command, description):
    """Run a shell command and print status"""
    print(f"\n{'='*60}")
    print(f" {description}")
    print(f"{'='*60}")

    try:
        result = subprocess.run(
            command,
            shell=True,
            check=True,
            capture_output=True,
            text=True
        )
        print(result.stdout)
        print(f" {description} - SUCCESS")
        return True
    except subprocess.CalledProcessError as e:
        print(f" {description} - FAILED")
        print(f"Error: {e.stderr}")
        return False

def main():
    print("""
    ╔════════════════════════════════════════════════════════════╗
    ║   Nigerian Tax RAG Service - Complete Setup                ║
    ╚════════════════════════════════════════════════════════════╝
    """)

    # Get project root
    project_root = Path(__file__).resolve().parent.parent

    # Step 1: Install Python dependencies
    print("\n STEP 1: Installing Python dependencies...")
    requirements_file = project_root / "requirements.txt"

    if not run_command(
            f"pip install -r {requirements_file}",
            "Installing Python packages from requirements.txt"
    ):
        print("\n  Python package installation failed!")
        return False

    # Step 2: Check if Ollama is installed
    print("\n STEP 2: Checking Ollama installation...")

    try:
        subprocess.run(["ollama", "--version"],
                       check=True,
                       capture_output=True)
        print(" Ollama is installed")
    except (subprocess.CalledProcessError, FileNotFoundError):
        print(" Ollama is NOT installed!")
        print("\nPlease install Ollama from: https://ollama.ai/download")
        print("After installing, run this setup script again.")
        return False

    # Step 3: Pull embedding model
    print("\n STEP 3: Pulling AI models...")

    if not run_command(
            "ollama pull nomic-embed-text",
            "Downloading nomic-embed-text (embedding model - ~274 MB)"
    ):
        print("\n  Failed to pull nomic-embed-text model")
        return False

    # Step 4: Pull chat model
    if not run_command(
            "ollama pull llama3",
            "Downloading llama3 (chat model - ~4.7 GB)"
    ):
        print("\n  Failed to pull llama3 model")
        return False

    # Step 5: Verify models
    print("\n STEP 4: Verifying installation...")

    try:
        result = subprocess.run(
            ["ollama", "list"],
            check=True,
            capture_output=True,
            text=True
        )
        print("\n Installed Ollama models:")
        print(result.stdout)
    except subprocess.CalledProcessError as e:
        print(f" Could not list models: {e}")
        return False

    # Success!
    print(f"\n{'='*60}")
    print(" Setup Complete!")
    print(f"{'='*60}")
    print("\nNext steps:")
    print("1. Make sure Docker is running: docker-compose up -d db")
    print("2. Initialize database: python scripts/init_db.py")
    print("3. Upload documents: python scripts/upload_document.py <pdf_path>")
    print("4. Start API server: uvicorn app.main:app --reload")
    print(f"{'='*60}\n")

    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)