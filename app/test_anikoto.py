import urllib.request
import json
import re
import urllib.parse

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "X-Requested-With": "XMLHttpRequest",
    "Referer": "https://anikoto.cz/"
}

def get(url, is_ajax=False):
    req_headers = headers.copy() if is_ajax else {"User-Agent": headers["User-Agent"]}
    req = urllib.request.Request(url, headers=req_headers)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.read().decode("utf-8")
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return ""

print("1. Search for naruto...")
html = get("https://anikoto.cz/filter?keyword=naruto")
matches = re.findall(r"href=[\"']/watch/([^\"']+)[\"']", html)
print("Found watch slugs:", list(set(matches))[:5])

if matches:
    slug = matches[0]
    print(f"\n2. Fetching watch page for {slug}...")
    watch_html = get(f"https://anikoto.cz/watch/{slug}")
    
    show_id_match = re.search(r"name=[\"']show_id[\"']\s+value=[\"'](\d+)[\"']", watch_html) or re.search(r"data-id=[\"'](\d+)[\"']", watch_html)
    numeric_id = show_id_match.group(1) if show_id_match else ""
    print("Numeric Show ID:", numeric_id)

    if numeric_id:
        print(f"\n3. Fetching episode list for numeric ID {numeric_id}...")
        ep_json = get(f"https://anikoto.cz/ajax/episode/list/{numeric_id}", is_ajax=True)
        try:
            data = json.loads(ep_json)
            ep_html = data.get("result", "")
        except:
            ep_html = ep_json

        data_ids_list = re.findall(r"data-ids=[\"']([^\"']+)[\"']", ep_html)
        print("Found episode data-ids:", data_ids_list[:5])

        if data_ids_list:
            data_id = data_ids_list[0]
            print(f"\n4. Fetching server list for data-ids={data_id}...")
            parts = data_id.split("&eps=")
            servers_json = get(f"https://anikoto.cz/ajax/server/list?servers={parts[0]}&eps={parts[1]}", is_ajax=True)
            try:
                s_data = json.loads(servers_json)
                s_html = s_data.get("result", "")
            except:
                s_html = servers_json

            print("\nServer HTML:", s_html)

            link_ids = re.findall(r"data-link-id=[\"']([^\"']+)[\"']", s_html)
            for link_id in link_ids:
                print(f"\n--- Fetching server linkId: {link_id} ---")
                src_json = get(f"https://anikoto.cz/ajax/server?get={link_id}", is_ajax=True)
                print("Server response:", src_json)
                try:
                    src_obj = json.loads(src_json)
                    embed_url = src_obj.get("result", {}).get("url") or src_obj.get("url")
                    print("Embed URL:", embed_url)

                    if embed_url:
                        embed_host = "/".join(embed_url.split("/")[:3])
                        embed_page = get(embed_url)
                        embed_id_match = re.search(r"data-id=[\"']([^\"']+)[\"']", embed_page) or re.search(r"id=[\"']([^\"']+)[\"']", embed_page)
                        if embed_id_match:
                            embed_id = embed_id_match.group(1)
                            print("Extracted Embed ID:", embed_id)
                            get_src_url = f"{embed_host}/stream/getSources?id={urllib.parse.quote(embed_id)}"
                            req = urllib.request.Request(get_src_url, headers={
                                "User-Agent": headers["User-Agent"],
                                "X-Requested-With": "XMLHttpRequest",
                                "Referer": f"{embed_host}/"
                            })
                            try:
                                with urllib.request.urlopen(req) as resp:
                                    print("getSources result:", resp.read().decode("utf-8"))
                            except Exception as ex:
                                print("getSources err:", ex)
                except Exception as ex:
                    print("Parse err:", ex)
