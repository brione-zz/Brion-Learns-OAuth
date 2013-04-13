package com.example.bloa;

// You can write your own provider or override the values here
public class DefaultKeysProvider implements KeysProvider {

    @Override
    public String getKey1() {
        // TODO: Set this to your App's Consumer key
        return "Your App's Consumer Key";
    }

    @Override
    public String getKey2() {
        // TODO: Set this to your App's Consumer secret
        return "App Consumer Secret";
    }
}
