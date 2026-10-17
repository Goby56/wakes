# FancyMenu compatibility investigation (issue #215)

**Status:** resolved. Root cause was in Wakes; fix applied.

## Report

[#215](https://github.com/Goby56/wakes/issues/215): the game crashes during loading
with Wakes and FancyMenu installed (MC 26.2+). Cross-referenced by
[FancyMenu#1733](https://github.com/Keksuccino/FancyMenu/issues/1733). The crash
only happens on Fabric 26.1.1+, not NeoForge, which points to an access widener.

```
IncompatibleClassChangeError: class de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider
overrides final method net.minecraft.client.gui.components.AbstractSliderButton.getHandleSprite()
```

## Root cause

`wakes.accesswidener` used `accessible` on `AbstractSliderButton.getSprite()` and
`getHandleSprite()`, which are `private` in vanilla (verified with `javap` on
26.1 through 26.3).

Per the [Fabric spec](https://docs.fabricmc.net/develop/class-tweakers/access-widening):

- `accessible` makes a method public, and **final if it was private**.
- `extendable` makes it non-final and at least protected.

So Wakes turned both methods into `public final`. Access wideners apply globally
to the single loaded vanilla class, so this affected every mod.

FancyMenu has no access widener and defines same-named methods without
`@Override`. Against vanilla these are unrelated methods, since a private method
cannot be overridden. Once Wakes made the parent method public, FancyMenu's
methods became overrides of a final method, and the JVM rejected the class.

FancyMenu's maintainer confirmed this on #1733, suggested `extendable` or a Mixin
invoker, and marked Wakes as incompatible.

Neither mod did anything wrong in isolation. Reusing a private method's name is
safe in Java, and requesting minimal access is good practice. The trap is that
`accessible` on a private method is the one choice that makes it take part in
dispatch while forbidding overrides, and Fabric merges it for all mods.

An earlier theory looked at AW merge semantics between two mods' entries, but
FancyMenu has no entry. The issue was per-directive semantics, not merging.

## Fix

Changed both entries to `extendable`:

```
extendable method net/minecraft/client/gui/components/AbstractSliderButton getSprite ()Lnet/minecraft/resources/Identifier;
extendable method net/minecraft/client/gui/components/AbstractSliderButton getHandleSprite ()Lnet/minecraft/resources/Identifier;
```

Wakes only calls these from its own subclasses (`ColorIntervalSlider`,
`ColorPickerSlider`), which still works. `./gradlew compileJava` passes (requires
`JAVA_HOME` set to JDK 25).

## Alternatives (not applied)

1. **Reimplement locally**, as FancyMenu does. The vanilla logic is trivial:
   ```
   getSprite():       active && focused && !canChangeValue     ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE
   getHandleSprite(): active && (isHovered || !canChangeValue) ? SLIDER_HANDLE_HIGHLIGHTED_SPRITE : SLIDER_HANDLE_SPRITE
   ```
   Removes the AW entry, but may drift from vanilla.
2. **Mixin `@Invoker`**. Calls the private method without changing its access
   flags, so no other mod is affected. The most surgical option, and worth
   preferring when Wakes only needs to call a private vanilla member.
