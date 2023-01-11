# -*- coding: utf-8 -*-
"""
Retrieves the link to Spotify's CDN (i.scdn.co) for each album in the playlist by first fetching the OEmbed link for each track.
@author Vadim Hagedorn
"""
# -------------------------------- watermark --------------------------------
def _watermark(): # in a function to hide variables
    _name = "cover art URL parser" 
    _author ="by Vadim Hagedorn [Sat Feb  5 19:20:24 2022]"
    _spacer = '-' * len(_author)
    print(_spacer, '\n', _name, '\n', _author, '\n', _spacer)
_watermark()
# ---------------------------------------------------------------------------

import requests
import sys

file = open('content/playlist.txt', 'r')
lines = file.readlines()

base = "https://open.spotify.com/oembed?url=spotify:track:"

#https://open.spotify.com/track/TRACK_ID?si=PLAYLIST_ID

urls = dict()

for line in lines:
    link = line.split('?si=')[0].replace('\n', '')

    # don't include locally imported files
    if '/local/' in link:
        continue
    
    print("link =", link)
    link = base + link.split('/track/')[1]

    # fetch the embed
    resp = requests.get(link)
    try:
        json = resp.json()
    except:
        print("url =", link)
        continue

    # get the album cover link
    url = json["thumbnail_url"]
    
    urls[url] = 1
    
    print(url)

print(f"\nParsed {len(urls)} image links from {len(lines)} tracks.")

# save

lines = []
for url in urls:
    #print(url)
    lines.append(url + '\n')
    
with open('content/links.txt', 'a') as file:
    file.writelines(lines)