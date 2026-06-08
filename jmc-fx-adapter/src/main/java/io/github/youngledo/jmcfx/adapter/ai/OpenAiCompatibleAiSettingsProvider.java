package io.github.youngledo.jmcfx.adapter.ai;

@FunctionalInterface
public interface OpenAiCompatibleAiSettingsProvider {
    OpenAiCompatibleAiSettings load();
}
