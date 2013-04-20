package com.eyebrowssoftware.bloa.test;

import com.eyebrowssoftware.bloa.IKeysProvider;

public class DefaultKeysProvider extends Object implements IKeysProvider {

    @Override
    public String getKey1() {
        // TODO Generate or look these up on Twitter
        return "Generate or look these up";
    }

    @Override
    public String getKey2() {
        // TODO Generate or look these up on Twitter
        return "Generate or look these up";
    }

}
