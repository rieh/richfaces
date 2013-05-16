package org.richfaces.photoalbum.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.richfaces.json.JSONArray;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;
import org.richfaces.photoalbum.event.ErrorEvent;
import org.richfaces.photoalbum.event.EventType;
import org.richfaces.photoalbum.event.Events;

@Named
@ApplicationScoped
public class FacebookAlbumCache {

    @Inject
    @EventType(Events.ADD_ERROR_EVENT)
    Event<ErrorEvent> error;

    private Map<String, JSONObject> albums = new HashMap<String, JSONObject>();
    // < albumId, {album} >
    private Map<String, Map<String, JSONObject>> images = new HashMap<String, Map<String, JSONObject>>();
    // < albumId, < imageId, {image} > >

    private boolean needsUpdate = true;

    private String currentAlbumId;
    private String currentPhotoId;

    public void setAll(String json) {
        try {
            JSONObject jo = new JSONObject(json);
            if (!jo.has("albums")) {
                return;
            }

            storeAlbums(jo.getJSONArray("albums"));
            storeImagesToAlbum(jo.getJSONArray("images"));
            setNeedsUpdate(true);
        } catch (JSONException e) {
            error.fire(new ErrorEvent("Error: ", e.getMessage()));
        }
    }

    public void storeAlbums(JSONArray ja) {
        storeAlbums(ja, false);
    }

    public void storeAlbums(JSONArray ja, boolean rewrite) {
        String albumId;
        JSONObject jo;

        try {
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);

                if (!jo.has("aid")) {
                    error.fire(new ErrorEvent("Error, object does not contain albums"));
                }

                albumId = jo.getString("aid");

                if (albums.containsKey(albumId) && !rewrite) {
                    // the album has already been loaded
                    return;
                } else if (rewrite) {
                    albums.remove(albumId);
                }
                images.put(albumId, null);
                albums.put(albumId, jo);
            }
        } catch (JSONException je) {
            error.fire(new ErrorEvent("Error", je.getMessage()));
        }
    }

    public void storeImagesToAlbum(JSONArray ja) {

        String imageId = "";
        String albumId = "";
        JSONObject jo;

        try {
            for (int i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);

                if (!jo.has("aid") || !jo.has("pid")) {
                    error.fire(new ErrorEvent("Error, object does not contain images"));

                }

                albumId = jo.getString("aid");
                imageId = jo.getString("pid");

                if (images.get(albumId) == null) {
                    images.put(albumId, new HashMap<String, JSONObject>());
                }

                images.get(albumId).put(imageId, jo);
            }
        } catch (JSONException je) {
            error.fire(new ErrorEvent("Error", je.getMessage()));
        }
    }

    public JSONObject getAlbum(String albumId) {
        return albums.get(albumId);
    }

    public Map<String, JSONObject> getImagesOfAlbum(String albumId) {
        return images.get(albumId);
    }

    // takes a list of id's from an event
    public boolean areAlbumsLoaded(List<String> albumIds) {
        if (albumIds != null) {
            for (String id : albumIds) {
                if (images.get(id) == null) {
                    setNeedsUpdate(true);
                    return false;
                }
            }
        }

        setNeedsUpdate(false);
        return true;
    }

    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public String getCurrentAlbumId() {
        return currentAlbumId;
    }

    public void setCurrentAlbumId(String currentAlbumId) {
        this.currentAlbumId = currentAlbumId;
    }

    public String getCurrentPhotoId() {
        return currentPhotoId;
    }

    public void setCurrentPhotoId(String currentPhotoId) {
        this.currentPhotoId = currentPhotoId;
    }

    public JSONObject getCurrentAlbum() {
        return albums.get(currentAlbumId);
    }

    public List<JSONObject> getCurrentPhotos() {
        return new ArrayList<JSONObject>(images.get(currentAlbumId).values());
    }

    public JSONObject getCurrentPhoto() {
        return images.get(currentAlbumId).get(currentPhotoId);
    }
}
