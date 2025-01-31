package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.SystemUtils;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class NamedEntityWriteReadFix extends DataFix {

    private final String name;
    private final String entityName;
    private final TypeReference type;

    public NamedEntityWriteReadFix(Schema schema, boolean flag, String s, TypeReference typereference, String s1) {
        super(schema, flag);
        this.name = s;
        this.type = typereference;
        this.entityName = s1;
    }

    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(this.type);
        Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
        Type<?> type2 = this.getOutputSchema().getType(this.type);
        Type<?> type3 = this.getOutputSchema().getChoiceType(this.type, this.entityName);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        Type<?> type4 = ExtraDataFixUtils.patchSubType(type1, type, type2);

        return this.fix(type, type2, opticfinder, type3, type4);
    }

    private <S, T, A, B> TypeRewriteRule fix(Type<S> type, Type<T> type1, OpticFinder<A> opticfinder, Type<B> type2, Type<?> type3) {
        return this.fixTypeEverywhere(this.name, type, type1, (dynamicops) -> {
            return (object) -> {
                Typed<S> typed = new Typed(type, dynamicops, object);

                return typed.update(opticfinder, type2, (object1) -> {
                    Typed<A> typed1 = new Typed(type3, dynamicops, object1);

                    return SystemUtils.writeAndReadTypedOrThrow(typed1, type2, this::fix).getValue();
                }).getValue();
            };
        });
    }

    protected abstract <T> Dynamic<T> fix(Dynamic<T> dynamic);
}
