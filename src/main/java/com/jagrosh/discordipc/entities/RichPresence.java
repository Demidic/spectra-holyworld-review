package com.jagrosh.discordipc.entities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.OffsetDateTime;
import org.json.JSONArray;
import org.json.JSONObject;
public class RichPresence
{
    private final String name;
    private final int type;
    private final String state;
    private final String details;
    private final OffsetDateTime startTimestamp;
    private final OffsetDateTime endTimestamp;
    private final String largeImageKey;
    private final String largeImageText;
    private final String smallImageKey;
    private final String smallImageText;
    private final String partyId;
    private final int partySize;
    private final int partyMax;
    private final String matchSecret;
    private final String joinSecret;
    private final String spectateSecret;
    private final boolean instance;
    private final List<Button> buttons;
    public RichPresence(String name, int type, String state, String details, OffsetDateTime startTimestamp, OffsetDateTime endTimestamp,
            String largeImageKey, String largeImageText, String smallImageKey, String smallImageText,
            String partyId, int partySize, int partyMax, String matchSecret, String joinSecret,
            String spectateSecret, boolean instance, List<Button> buttons)
    {
        this.name = name;
        this.type = type;
        this.state = state;
        this.details = details;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.largeImageKey = largeImageKey;
        this.largeImageText = largeImageText;
        this.smallImageKey = smallImageKey;
        this.smallImageText = smallImageText;
        this.partyId = partyId;
        this.partySize = partySize;
        this.partyMax = partyMax;
        this.matchSecret = matchSecret;
        this.joinSecret = joinSecret;
        this.spectateSecret = spectateSecret;
        this.instance = instance;
        this.buttons = buttons == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(buttons));
    }
    public JSONObject toJson()
    {
        JSONObject json = new JSONObject()
                .put("type", type)
                .put("instance", instance);
        putIfNotBlank(json, "name", name);
        putIfNotBlank(json, "state", state);
        putIfNotBlank(json, "details", details);
        JSONObject timestamps = new JSONObject();
        if(startTimestamp != null)
            timestamps.put("start", startTimestamp.toEpochSecond());
        if(endTimestamp != null)
            timestamps.put("end", endTimestamp.toEpochSecond());
        if(!timestamps.isEmpty())
            json.put("timestamps", timestamps);
        JSONObject assets = new JSONObject();
        putIfNotBlank(assets, "large_image", largeImageKey);
        putIfNotBlank(assets, "large_text", largeImageText);
        putIfNotBlank(assets, "small_image", smallImageKey);
        putIfNotBlank(assets, "small_text", smallImageText);
        if(!assets.isEmpty())
            json.put("assets", assets);
        if(partyId != null)
        {
            json.put("party", new JSONObject()
                    .put("id", partyId)
                    .put("size", new JSONArray().put(partySize).put(partyMax)));
        }
        if(matchSecret != null || joinSecret != null || spectateSecret != null)
        {
            json.put("secrets", new JSONObject()
                    .put("join", joinSecret)
                    .put("spectate", spectateSecret)
                    .put("match", matchSecret));
        }
        if(!buttons.isEmpty())
        {
            JSONArray buttonArray = new JSONArray();
            for(Button button : buttons)
                buttonArray.put(button.toJson());
            json.put("buttons", buttonArray);
        }
        return json;
    }
    private static void putIfNotBlank(JSONObject json, String key, String value)
    {
        if(value != null && !value.trim().isEmpty())
            json.put(key, value);
    }
    public static class Builder
    {
        private String name;
        private int type = 0;
        private String state;
        private String details;
        private OffsetDateTime startTimestamp;
        private OffsetDateTime endTimestamp;
        private String largeImageKey;
        private String largeImageText;
        private String smallImageKey;
        private String smallImageText;
        private String partyId;
        private int partySize;
        private int partyMax;
        private String matchSecret;
        private String joinSecret;
        private String spectateSecret;
        private boolean instance;
        private final List<Button> buttons = new ArrayList<>();
        public RichPresence build()
        {
            return new RichPresence(name, type, state, details, startTimestamp, endTimestamp,
                    largeImageKey, largeImageText, smallImageKey, smallImageText,
                    partyId, partySize, partyMax, matchSecret, joinSecret,
                    spectateSecret, instance, buttons);
        }
        public Builder setName(String name)
        {
            this.name = name;
            return this;
        }
        public Builder setType(int type)
        {
            this.type = type;
            return this;
        }
        public Builder setState(String state)
        {
            this.state = state;
            return this;
        }
        public Builder setDetails(String details)
        {
            this.details = details;
            return this;
        }
        public Builder setStartTimestamp(OffsetDateTime startTimestamp)
        {
            this.startTimestamp = startTimestamp;
            return this;
        }
        public Builder setEndTimestamp(OffsetDateTime endTimestamp)
        {
            this.endTimestamp = endTimestamp;
            return this;
        }
        public Builder setLargeImage(String largeImageKey, String largeImageText)
        {
            this.largeImageKey = largeImageKey;
            this.largeImageText = largeImageText;
            return this;
        }
        public Builder setLargeImage(String largeImageKey)
        {
            return setLargeImage(largeImageKey, null);
        }
        public Builder setSmallImage(String smallImageKey, String smallImageText)
        {
            this.smallImageKey = smallImageKey;
            this.smallImageText = smallImageText;
            return this;
        }
        public Builder setSmallImage(String smallImageKey)
        {
            return setSmallImage(smallImageKey, null);
        }
        public Builder setParty(String partyId, int partySize, int partyMax)
        {
            this.partyId = partyId;
            this.partySize = partySize;
            this.partyMax = partyMax;
            return this;
        }
        public Builder setMatchSecret(String matchSecret)
        {
            this.matchSecret = matchSecret;
            return this;
        }
        public Builder setJoinSecret(String joinSecret)
        {
            this.joinSecret = joinSecret;
            return this;
        }
        public Builder setSpectateSecret(String spectateSecret)
        {
            this.spectateSecret = spectateSecret;
            return this;
        }
        public Builder setInstance(boolean instance)
        {
            this.instance = instance;
            return this;
        }
        public Builder addButton(String label, String url)
        {
            if(buttons.size() >= 2)
                throw new IllegalStateException("Rich Presence supports at most 2 buttons.");
            buttons.add(new Button(label, url));
            return this;
        }
        public Builder clearButtons()
        {
            buttons.clear();
            return this;
        }
    }
    public static class Button
    {
        private final String label;
        private final String url;
        public Button(String label, String url)
        {
            if(label == null || label.trim().isEmpty())
                throw new IllegalArgumentException("Button label cannot be empty.");
            if(url == null || url.trim().isEmpty())
                throw new IllegalArgumentException("Button url cannot be empty.");
            this.label = label.trim();
            this.url = url.trim();
        }
        private JSONObject toJson()
        {
            return new JSONObject()
                    .put("label", label)
                    .put("url", url);
        }
    }
}
