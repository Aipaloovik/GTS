package com.nickimpact.gts.entries.pixelmon;

import com.nickimpact.gts.GTS;
import com.nickimpact.gts.configuration.ConfigKeys;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HiddenPower;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.EnumSpecialTexture;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.EVsStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.IVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.enums.EnumPokemon;
import com.pixelmonmod.pixelmon.storage.NbtKeys;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.DecimalFormat;
import java.util.function.Function;

public enum EnumPokemonFields {

	NAME(pokemon -> {
		if(pokemon.isEgg) {
			return "Pokemon Egg";
		}

		if(GTS.getInstance().getConfig().get(ConfigKeys.MEMES)) {
			if (pokemon.getSpecies().equals(EnumPokemon.Psyduck)) {
				return "AnDwHaT5 (Psyduck)";
			}

			if (pokemon.getSpecies().equals(EnumPokemon.Bidoof)) {
				return "God himself (Bidoof)";
			}
		}

		return pokemon.getName();
	}),
	ABILITY(pokemon -> pokemon.getAbility().getLocalizedName()),
	NATURE(pokemon -> pokemon.getNature().name()),
	NATURE_INCREASED(pokemon -> "+" + toRep(pokemon.getNature().increasedStat)),
	NATURE_DECREASED(pokemon -> "-" + toRep(pokemon.getNature().decreasedStat)),
	GENDER(pokemon -> pokemon.getGender().name()),
	SHINY(pokemon -> {
		if(!pokemon.getIsShiny())
			return Text.EMPTY;

		return Text.of(TextColors.GRAY, "(", TextColors.GOLD, "Shiny", TextColors.GRAY, ")");
	}),

	GROWTH(pokemon -> pokemon.getGrowth().name()),
	LEVEL(pokemon -> {
		if(pokemon.isEgg) {
			return 1;
		}

		return pokemon.getLvl().getLevel();
	}),
	FORM(EntityPixelmon::getForm),
	FORM_NAME(pokemon -> {
		String form = pokemon.getFormEnum().getFormSuffix();
		if(form.startsWith("-")) {
			return form.substring(1);
		} else {
			return form;
		}
	}),
	CLONES(pokemon -> {
		if(pokemon.getSpecies().equals(EnumPokemon.Mew)) {
			return pokemon.getEntityData().getShort(NbtKeys.STATS_NUM_CLONED);
		}
		return 0;
	}),
	ENCHANTED(pokemon -> {
		switch (pokemon.getSpecies()) {
			case Mesprit:
			case Azelf:
			case Uxie:
				return pokemon.getEntityData().getShort(NbtKeys.STATS_NUM_ENCHANTED);
			default:
				return 0;
		}
	}),
	EV_PERCENT(pokemon -> new DecimalFormat("#0.##").format(totalEVs(pokemon.stats.evs) / 510.0 * 100) + "%"),
	IV_PERCENT(pokemon -> new DecimalFormat("#0.##").format(totalIVs(pokemon.stats.ivs) / 186.0 * 100) + "%"),
	EV_TOTAL(pokemon -> (int)totalEVs(pokemon.stats.evs)),
	IV_TOTAL(pokemon -> (int)totalIVs(pokemon.stats.ivs)),
	NICKNAME(pokemon -> TextSerializers.LEGACY_FORMATTING_CODE.deserialize(pokemon.getNickname())),
	EV_HP(pokemon -> pokemon.stats.evs.hp),
	EV_ATK(pokemon -> pokemon.stats.evs.attack),
	EV_DEF(pokemon -> pokemon.stats.evs.defence),
	EV_SPATK(pokemon -> pokemon.stats.evs.specialAttack),
	EV_SPDEF(pokemon -> pokemon.stats.evs.specialDefence),
	EV_SPEED(pokemon -> pokemon.stats.evs.speed),
	IV_HP(pokemon -> pokemon.stats.ivs.HP),
	IV_ATK(pokemon -> pokemon.stats.ivs.Attack),
	IV_DEF(pokemon -> pokemon.stats.ivs.Defence),
	IV_SPATK(pokemon -> pokemon.stats.ivs.SpAtt),
	IV_SPDEF(pokemon -> pokemon.stats.ivs.SpDef),
	IV_SPEED(pokemon -> pokemon.stats.ivs.Speed),
	SPECIAL_TEXTURE(pokemon -> {
		try {
			return EnumSpecialTexture.fromIndex(pokemon.getSpecialTextureIndex()).name();
		} catch (Exception e) {
			return "";
		}
	}),
	HIDDEN_POWER(pokemon -> HiddenPower.getHiddenPowerType(pokemon.stats.ivs).name()),
	MOVES_1(pokemon -> pokemon.getMoveset().attacks[0].baseAttack.getLocalizedName()),
	MOVES_2(pokemon -> pokemon.getMoveset().attacks[1].baseAttack.getLocalizedName()),
	MOVES_3(pokemon -> pokemon.getMoveset().attacks[2].baseAttack.getLocalizedName()),
	MOVES_4(pokemon -> pokemon.getMoveset().attacks[3].baseAttack.getLocalizedName()),
	SHINY_STATE(pokemon -> pokemon.getIsShiny() ? "Yes" : "No"),
	POKERUS_STATE(pokemon -> pokemon.getPokerus().isPresent() ? "Yes" : "No"),
	POKERUS(pokemon -> pokemon.getPokerus().isPresent() ? "PKRS" : null);

	public final Function<EntityPixelmon, Object> function;

	private EnumPokemonFields(Function<EntityPixelmon, Object> function) {
		this.function = function;
	}

	private static double totalEVs(EVsStore evs) {
		return evs.hp + evs.attack + evs.defence + evs.specialAttack + evs.specialDefence + evs.speed;
	}

	private static double totalIVs(IVStore ivs) {
		return ivs.HP + ivs.Attack + ivs.Defence + ivs.SpAtt + ivs.SpDef + ivs.Speed;
	}

	private static String toRep(StatsType stat) {
		switch(stat) {
			case HP:
				return "HP";
			case Attack:
				return "Atk";
			case Defence:
				return "Def";
			case SpecialAttack:
				return "SpAtk";
			case SpecialDefence:
				return "SpDef";
			case Speed:
				return "Speed";
			default:
				return "???";
		}
	}
}
