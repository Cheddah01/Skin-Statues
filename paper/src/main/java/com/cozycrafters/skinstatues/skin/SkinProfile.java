package com.cozycrafters.skinstatues.skin;

import com.cozycrafters.skinstatues.model.SkinModel;
import java.net.URI;
import java.util.UUID;

/**
 * The result of a profile lookup: who the player is and where their skin lives.
 *
 * @param uuid    the account UUID
 * @param name    the correctly cased account name
 * @param skinUrl the skin texture URL from the profile's texture property
 * @param model   the arm shape declared by the profile, or the UUID default
 */
public record SkinProfile(UUID uuid, String name, URI skinUrl, SkinModel model) {
}
