package org.sunflow.core.modifiers;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Modifier;
import org.sunflow.core.ParameterList;
import org.sunflow.core.ShadingState;
import org.sunflow.core.Texture;
import org.sunflow.core.TextureCache;
import org.sunflow.math.OrthoNormalBasis;

public class NormalMapModifier implements Modifier {

    private Texture normalMap;

    public NormalMapModifier() {
        normalMap = null;
    }

    @Override
    public boolean update(ParameterList pl, SunflowAPI api) {
        String filename = pl.getString("texture", null);
        if (filename != null) {
            normalMap = TextureCache.getTexture(api.resolveTextureFilename(filename), true);
        }
        return normalMap != null;
    }

    @Override
    public void modify(ShadingState state) {
        // apply normal map
        state.getNormal().set(normalMap.getNormal(state.getUV().x, state.getUV().y, state.getBasis()));
        state.setBasis(OrthoNormalBasis.makeFromW(state.getNormal()));
    }
}