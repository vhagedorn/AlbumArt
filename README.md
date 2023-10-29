## About

Album art collage generator.
I was bored so I made this to get a wallpaper for my phone.

This accepts Spotify playlists and converts them into collages for any given resolution as shown below.

## Examples

With the UI, it is very easy to edit, preview, and crop the collage before saving it to a file.

Have a look at a few screenshots.

### UI

* Main window

![Main UI](https://imgur.com/DAHOILg.png)

* Scaling (AUTO will fit the maximum amount of images inside the given output resolution)

![Scaling](https://imgur.com/k9FjPDZ.gif)

* Easy pan & zoom preview

![Pan and Save](https://imgur.com/c4bNRPh.gif)

* Results

![Result (downscaled)](https://imgur.com/R3LOqpd.png)


<details>
  <summary>Generated Image (full resolution)</summary>
  
![iPhone](https://imgur.com/D0MWgXb.png)
</details>

## Usage
**Please run the following commands from the project directory (the cloned directory).**
1. Clone the repo via `git clone https://github.com/vhagedorn/AlbumArt`.
2. Create a text file called `content/playlist.txt` with the Spotify links that you wish to get.
3. Open a terminal/powershell _at the project directory_ (probably `AlbumArt`).
	- Option A: (for Linux/MacOS users)
		1. Make sure your links are in `playlist.txt`!
   		2. Run `./fetch_content` and grab a cup of coffee.
	- Option B: (for Windows users)
 		1. Find the links to the album covers with `python3 src/main/python/parse.py`.
		2. Run `python3 src/main/python/download.py`, which will download the album art as `.jfif` files.
4. Finally, you can generate the collage via `java -jar target/AlbumArt.jar`.
   
   Follow its instructions to generate as many collages as you want!

## Explanation
Initially in Java, it spread to a mess of multiple languages after several attempts.

### Python
#### `parse.py`
- Converts `content/playlist.txt` (list Spotify links) -> `content/links.txt` (list of links to album art)

This abuses Spotify's OEmbed service, which provides the URL to their album cover CDN.
An example request would return JSON containing `thumbnail_url`.
`https://open.spotify.com/oembed?url=spotify:track:6c5wQFfJApRMooKE7UQnlH`
returns:
```json
{
  "html": "<iframe style=\"border-radius: 12px\" width=\"100%\" height=\"80\" title=\"Spotify Embed: durag activity (with Travis Scott)\" frameborder=\"0\" allowfullscreen allow=\"autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture\" src=\"https://open.spotify.com/embed/track/6c5wQFfJApRMooKE7UQnlH?utm_source=oembed\"></iframe>",
  "width": 456,
  "height": 80,
  "version": "1.0",
  "provider_name": "Spotify",
  "provider_url": "https://spotify.com",
  "type": "rich",
  "title": "durag activity (with Travis Scott)",
  "thumbnail_url": "https://i.scdn.co/image/ab67616d00001e021bfa23b13d0504fb90c37b39", # bingo!
  "thumbnail_width": 300,
  "thumbnail_height": 300
}
```

#### `download.py`
- Downloads the album art into `content/revisions/py 3/pics` as `.jfif` files

No explanation really needed... Essentially 3 lines of code:
```python
r = requests.get(url, headers=user_agent, allow_redirects=True, stream=True)
    with open(filename, 'wb') as f:
        shutil.copyfileobj(r.raw, f)
```
### Java
#### `AlbumArt.java` 
- Combines the downloaded pictures into a collage.

It basically loops through the enclosing square, only placing a photo if it's within the frame to maximize the number of pictures that can fit inside.
Here's a diagram of what it's doing:
![collage diagram](https://imgur.com/pRiwjHv.png)

And here is the main part of the code.
```java
		int   i    = 0;
		float last = 0;
		for (double x = 0; x < d / scale; x++) {//iterate over (unscaled) X from a little outside {-(d/scale)/2} until the diagonal {d/scale}
			int r = rand ? (int) Math.round(Math.random() * scale) : 0;
			for (double y = last -= 1; y < (d / scale) + last; y++) {//iterate over (unscaled) Y, staring further outside each time in order to fill the rect
				transform = g2d.getTransform();

				int xc = (int) Math.round(x * scale),
						yc = (int) Math.round(y * scale);

				double deltaX = x * Math.cos(theta) - y * Math.sin(theta),
						deltaY = x * Math.sin(theta) + y * Math.cos(theta);

				deltaX *= scale;
				deltaY *= scale;

				//only proceed if we're in bounds
				boolean inB = inBounds(deltaX, deltaY, w, h) ||
							  inBounds(deltaX - scale, deltaY + scale, w, h) ||
							  inBounds(deltaX - scale, deltaY, w, h) ||
							  inBounds(deltaX, deltaY + scale, w, h);

				if (inB) {
					g2d.setColor(Color.WHITE);
					//rotate around origin

					//draw image on rotated coordinate
					if (i < images.length) g2d.drawImage(images[i++], xc, yc + r, scale, scale, null);
				}

				g2d.setTransform(transform);
			}
		}
```

---

## End
Hopefully someone found this interesting!

