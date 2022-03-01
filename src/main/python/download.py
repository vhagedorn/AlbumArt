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

if not os.path.exists('content/revisions/py 3/pics'):
    os.mkdir('content/revisions/py 3/pics')

user_agent = {'User-agent': 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36'}

#https://i.scdn.co/image/ALBUM_ID
for url in lines:
    url = url.replace('\n', '')
    print(url)
    
    filename = 'content/revisions/py 3/pics' + url.split("/image/")[1] + '.jfif'
    if os.path.exists(filename):
        continue

    # fetch 'n' save
    r = requests.get(url, headers=user_agent, allow_redirects=True, stream=True)
    with open(filename, 'wb') as f:
        shutil.copyfileobj(r.raw, f)