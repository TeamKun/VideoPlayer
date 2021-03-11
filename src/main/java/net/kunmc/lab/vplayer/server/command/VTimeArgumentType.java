package net.kunmc.lab.vplayer.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class VTimeArgumentType implements ArgumentType<VTimeArgumentType.VTime> {
    private static final Collection<String> unitDefaults = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType exceptionInvalidUnit = new SimpleCommandExceptionType(new TranslationTextComponent("argument.time.invalid_unit"));
    private static final DynamicCommandExceptionType exceptionTickCount = new DynamicCommandExceptionType(count -> new TranslationTextComponent("argument.time.invalid_tick_count", count));
    private static final Object2FloatMap<String> units = new Object2FloatOpenHashMap<>();

    public static VTimeArgumentType timeArg() {
        return new VTimeArgumentType();
    }

    public static VTime getTime(final CommandContext<?> context, final String name) {
        return context.getArgument(name, VTime.class);
    }

    public VTime parse(StringReader text) throws CommandSyntaxException {
        float f = text.readFloat();
        if (text.canRead() && text.peek() == '%') {
            text.readStringUntil('%');
            if (f < 0 || f > 100)
                throw exceptionTickCount.create(f);
            return new VTime(VTimeType.PERCENT, f);
        } else {
            float sec = 0;
            while (true) {
                String s = text.readUnquotedString();
                float i = units.getOrDefault(s, 0);
                if (i == 0)
                    throw exceptionInvalidUnit.create();

                sec += f * i;

                text.skipWhitespace();
                if (text.canRead())
                    f = text.readFloat();
                else
                    break;
            }
            return new VTime(VTimeType.SECONDS, sec);
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder sb) {
        StringReader stringreader = new StringReader(sb.getRemaining());

        try {
            stringreader.readFloat();
        } catch (CommandSyntaxException var5) {
            return sb.buildFuture();
        }

        return ISuggestionProvider.suggest(units.keySet(), sb.createOffset(sb.getStart() + stringreader.getCursor()));
    }

    public Collection<String> getExamples() {
        return unitDefaults;
    }

    static {
        units.put("h", 60 * 60);
        units.put("m", 60);
        units.put("s", 1);
        units.put("%", 0);
        units.put("", 1);
    }

    public static class VTime {
        public final VTimeType type;
        public final float value;

        public VTime(VTimeType type, float value) {
            this.type = type;
            this.value = value;
        }

        public float getTime(float duration) {
            if (type == VTimeType.PERCENT)
                if (duration <= 0)
                    return 0;
                else
                    return value / 100f * duration;
            return value;
        }
    }

    public enum VTimeType {
        SECONDS,
        PERCENT,
    }
}
