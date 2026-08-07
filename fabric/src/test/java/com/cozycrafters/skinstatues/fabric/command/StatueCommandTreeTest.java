package com.cozycrafters.skinstatues.fabric.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class StatueCommandTreeTest {
    @Test
    void commandTreeContainsOnlyGenerateAndUndoShapes() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        new StatueCommand(() -> null, () -> 4).register(dispatcher);

        var statue = dispatcher.getRoot().getChild("statue");
        assertNotNull(statue);
        assertEquals(Set.of("undo", "name"), names(statue));
        assertEquals(Set.of("scale"), names(statue.getChild("name")));
        assertEquals(Set.of(), names(statue.getChild("undo")));
    }

    private static Set<String> names(com.mojang.brigadier.tree.CommandNode<CommandSourceStack> node) {
        return node.getChildren().stream().map(com.mojang.brigadier.tree.CommandNode::getName)
                .collect(Collectors.toSet());
    }
}
