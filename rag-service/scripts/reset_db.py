"""
Reset database tables (drop and recreate)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.db import db, Documents, Tags, DocumentTags, DocumentInformationChunks

def main():
    print("=" * 60)
    print("Resetting Database Tables")
    print("=" * 60)

    try:
        db.connect()
        print("Connected to database")

        print("Dropping existing tables...")
        db.drop_tables([DocumentInformationChunks, DocumentTags, Tags, Documents])
        print("Tables dropped successfully")

        db.close()
        print("Database connection closed")

        print("=" * 60)
        print("Database reset complete!")
        print("Now run: python scripts/init_db.py")
        print("=" * 60)

        return True

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)