package com.cozycrafters.cozystatues.skin;

import com.cozycrafters.cozystatues.model.SkinModel;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerTextures;

/**
 * Profile lookups through Paper's own profile API.
 *
 * <p>Paper resolves the name against Mojang's account and session services and
 * keeps the answer in the server's profile cache, so repeat statues of the same
 * player usually cost nothing. The target player does not have to be online, or
 * to have ever joined. {@code complete(true, true)} forces the online-mode
 * lookup so the command still works on an offline-mode server.
 */
public final class PaperProfileSource implements ProfileSource {

    @Override
    public SkinProfile lookup(String name) throws SkinLookupException {
        PlayerProfile profile;
        try {
            profile = Bukkit.createProfile(name);
            profile.complete(true, true);
        } catch (RuntimeException ex) {
            throw new SkinLookupException("The skin service could not be reached. Try again in a moment.", ex);
        }

        if (profile.getId() == null) {
            throw new SkinLookupException("No Minecraft player named '" + name + "' was found.");
        }

        PlayerTextures textures = profile.getTextures();
        URL skin = textures == null ? null : textures.getSkin();
        if (skin == null) {
            throw new SkinLookupException("'" + profile.getName() + "' has no skin to build a statue from.");
        }

        URI uri;
        try {
            uri = skin.toURI();
        } catch (URISyntaxException ex) {
            throw new SkinLookupException("That player's skin data is malformed.", ex);
        }

        SkinModel model = switch (textures.getSkinModel()) {
            case SLIM -> SkinModel.SLIM;
            case CLASSIC -> SkinModel.CLASSIC;
            case null -> SkinModel.defaultFor(profile.getId());
        };
        String resolvedName = profile.getName() == null ? name : profile.getName();
        return new SkinProfile(profile.getId(), resolvedName, uri, model);
    }
}
