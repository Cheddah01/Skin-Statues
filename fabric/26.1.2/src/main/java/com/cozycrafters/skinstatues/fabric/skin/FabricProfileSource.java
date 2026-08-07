package com.cozycrafters.skinstatues.fabric.skin;

import com.cozycrafters.skinstatues.fabric.model.SkinModel;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import java.net.URI;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/** Resolves offline names through the server's native authenticated profile services. */
public final class FabricProfileSource implements ProfileSource {
    private final MinecraftServer server;

    public FabricProfileSource(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public SkinProfile lookup(String name) throws SkinLookupException {
        GameProfile profile;
        try {
            Optional<GameProfile> found = server.services().profileResolver().fetchByName(name);
            if (found.isEmpty()) {
                throw new SkinLookupException("No Minecraft player named '" + name + "' was found.");
            }
            profile = found.get();
        } catch (SkinLookupException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new SkinLookupException("The skin service could not be reached. Try again in a moment.", ex);
        }

        try {
            MinecraftProfileTextures textures = server.services().sessionService().getTextures(profile);
            MinecraftProfileTexture skin = textures.skin();
            if (skin == null) {
                throw new SkinLookupException("'" + profile.name() + "' has no skin to build a statue from.");
            }
            URI url = secureTextureUri(URI.create(skin.getUrl()));
            SkinModel model = "slim".equalsIgnoreCase(skin.getMetadata("model"))
                    ? SkinModel.SLIM : SkinModel.CLASSIC;
            return new SkinProfile(profile.id(), profile.name(), url, model);
        } catch (SkinLookupException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new SkinLookupException("That player's skin data is malformed.", ex);
        }
    }

    static URI secureTextureUri(URI uri) throws SkinLookupException {
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return uri;
        }
        if ("http".equalsIgnoreCase(uri.getScheme()) && "textures.minecraft.net".equalsIgnoreCase(uri.getHost())) {
            try {
                return new URI("https", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (Exception ex) {
                throw new SkinLookupException("That player's skin data is malformed.", ex);
            }
        }
        throw new SkinLookupException("That player's skin data is malformed.");
    }
}
