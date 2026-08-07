package com.cozycrafters.skinstatues.fabric.command;

import com.cozycrafters.skinstatues.fabric.statue.StatueRuntime;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** The intentionally narrow {@code /statue <name> <scale>} and {@code /statue undo} command. */
public final class StatueCommand {
    private final Supplier<StatueRuntime> runtime;
    private final IntSupplier maxScale;

    public StatueCommand(Supplier<StatueRuntime> runtime, IntSupplier maxScale) {
        this.runtime = runtime;
        this.maxScale = maxScale;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statue")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> error(context.getSource(), StatueRequest.USAGE))
                .then(Commands.literal("undo")
                        .executes(context -> withPlayer(context.getSource(), player -> service().undo(player))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemainingLowerCase();
                            context.getSource().getOnlinePlayerNames().stream()
                                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(remaining))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> error(context.getSource(), StatueRequest.USAGE))
                        .then(Commands.argument("scale", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (int scale = 1; scale <= maxScale.getAsInt(); scale++) {
                                        String value = Integer.toString(scale);
                                        if (value.startsWith(builder.getRemaining())) {
                                            builder.suggest(value);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> executeGenerate(context.getSource(),
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "scale"))))));
    }

    private int executeGenerate(CommandSourceStack source, String name, String scale) {
        StatueRequest.Result parsed = StatueRequest.parse(new String[]{name, scale}, maxScale.getAsInt());
        return switch (parsed) {
            case StatueRequest.Result.Ok(StatueRequest request) -> withPlayer(source,
                    player -> service().generate(player, request.playerName(), request.scale()));
            case StatueRequest.Result.Error(String message) -> error(source, message);
            case StatueRequest.Result.Undo ignored -> error(source, StatueRequest.USAGE);
        };
    }

    private int withPlayer(CommandSourceStack source, java.util.function.Consumer<ServerPlayer> action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return error(source, "Only players can build statues.");
        }
        StatueRuntime service = runtime.get();
        if (service == null) {
            return error(source, "SkinStatues is not ready yet.");
        }
        action.accept(player);
        return 1;
    }

    private StatueRuntime service() {
        return runtime.get();
    }

    private static int error(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }
}
