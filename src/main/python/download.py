# -*- coding: utf-8 -*-
"""
Downloads a bunch of links into files on the disk (pictures from Spotify's CDN `i.scdn.co`)
@author Vadim Hagedorn
"""
# -------------------------------- watermark --------------------------------
def _watermark(): # in a function to hide variables
    _name = "cover art DL" 
    _author ="by Vadim Hagedorn [Sat Feb  5 19:44:55 2022]"
    _spacer = '-' * len(_author)
    print(_spacer, '\n', _name, '\n', _author, '\n', _spacer)
_watermark()
# ---------------------------------------------------------------------------

import requests, os, shutil

# read the links
file = open('content/links.txt', 'r')
lines = file.readlines()

# prepare for download

DL_DIR = r'content/revisions/py 4/pics/'

if not DL_DIR.endswith('/'):
    DL_DIR += '/'

if not os.path.exists(DL_DIR):
    os.mkdir(DL_DIR)

user_agent = {'User-agent': 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36'}

i = 0
#https://i.scdn.co/image/ALBUM_ID
for url in lines:
    url = url.replace('\n', '')
    print(url)
    
    filename = DL_DIR + url.split("/image/")[1] + '.jfif'
    print(filename)
    if os.path.exists(filename):
        continue

    # fetch 'n' save
    r = requests.get(url, headers=user_agent, allow_redirects=True, stream=True)
    with open(filename, 'wb') as f:
        shutil.copyfileobj(r.raw, f)
    i += 1

print(f"\nDownloaded {i} new files from {len(lines)} links.")
