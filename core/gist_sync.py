import json
import time
from typing import Tuple, Dict, Any, Optional
import requests

GIST_FILENAME = "ownvoice_sync.json"

class GistSyncManager:
    """
    Cloud synchronization manager for OwnVoice using GitHub Gists.
    Syncs Personal Vocabulary Bank, Snippets, and Custom Tones between Windows PC and Android.
    """

    @staticmethod
    def sync_to_gist(github_token: str, payload: Dict[str, Any], gist_id: Optional[str] = None) -> Tuple[bool, str, Optional[str]]:
        if not github_token:
            return False, "GitHub Token is required for cloud sync.", None

        headers = {
            "Authorization": f"token {github_token.strip()}",
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "OwnVoice-CloudSync"
        }

        content_str = json.dumps(payload, indent=2, ensure_ascii=False)
        body = {
            "description": "OwnVoice Cloud Sync (Vocabulary & Snippets)",
            "public": False,
            "files": {
                GIST_FILENAME: {
                    "content": content_str
                }
            }
        }

        try:
            if gist_id and gist_id.strip():
                # Update existing Gist
                url = f"https://api.github.com/gists/{gist_id.strip()}"
                resp = requests.patch(url, json=body, headers=headers, timeout=10)
            else:
                # Create new private Gist
                url = "https://api.github.com/gists"
                resp = requests.post(url, json=body, headers=headers, timeout=10)

            if resp.status_code in (200, 201):
                data = resp.json()
                new_id = data.get("id")
                return True, f"Successfully synced to GitHub Gist! ({new_id[:8]}...)", new_id
            elif resp.status_code == 401:
                return False, "Invalid GitHub Token. Please verify permissions.", None
            elif resp.status_code == 404:
                return False, f"Gist ID '{gist_id}' not found.", None
            else:
                return False, f"GitHub API error ({resp.status_code}): {resp.text[:100]}", None
        except Exception as e:
            return False, f"Network error during sync: {e}", None

    @staticmethod
    def sync_from_gist(github_token: str, gist_id: str) -> Tuple[bool, str, Optional[Dict[str, Any]]]:
        if not gist_id or not gist_id.strip():
            return False, "Gist ID is required to pull sync data.", None

        headers = {
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "OwnVoice-CloudSync"
        }
        if github_token and github_token.strip():
            headers["Authorization"] = f"token {github_token.strip()}"

        url = f"https://api.github.com/gists/{gist_id.strip()}"
        try:
            resp = requests.get(url, headers=headers, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                files = data.get("files", {})
                if GIST_FILENAME in files:
                    content = files[GIST_FILENAME].get("content", "")
                    parsed = json.loads(content)
                    return True, "Successfully pulled data from GitHub Gist!", parsed
                else:
                    return False, f"File '{GIST_FILENAME}' not found in this Gist.", None
            elif resp.status_code == 404:
                return False, f"Gist ID '{gist_id}' not found.", None
            else:
                return False, f"GitHub API error ({resp.status_code})", None
        except Exception as e:
            return False, f"Network error: {e}", None
