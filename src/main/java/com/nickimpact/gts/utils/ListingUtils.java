package com.nickimpact.gts.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nickimpact.gts.GTS;
import com.nickimpact.gts.GTSInfo;
import com.nickimpact.gts.api.events.PurchaseEvent;
import com.nickimpact.gts.api.listings.entries.EntryHolder;
import com.nickimpact.gts.api.listings.entries.Minable;
import com.nickimpact.gts.api.listings.pricing.*;
import com.nickimpact.gts.configuration.ConfigKeys;
import com.nickimpact.gts.configuration.MsgConfigKeys;
import com.nickimpact.gts.api.events.ListEvent;
import com.nickimpact.gts.api.listings.Listing;
import com.nickimpact.gts.api.utils.MessageUtils;
import com.nickimpact.gts.discord.Message;
import com.nickimpact.gts.entries.prices.MoneyPrice;
import com.nickimpact.gts.logs.Log;
import com.nickimpact.gts.logs.LogAction;
import io.github.nucleuspowered.nucleus.api.exceptions.NucleusException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * (Some note will go here)
 *
 * @author NickImpact
 */
public class ListingUtils {

	public static void addToMarket(Listing listing) {
		ListEvent listEvent = new ListEvent(
				null,
				listing,
				Sponge.getCauseStackManager().getCurrentCause()
		);
		Sponge.getEventManager().post(listEvent);

		if(!listEvent.isCancelled()) {
			GTS.getInstance().getStorage().addListing(listing);
			GTS.getInstance().getListingsCache().add(listing);

			// Broadcast a message to everyone but the player who deposited the listing and the ignorers
			Set<Player> players = Sponge.getServer().getOnlinePlayers().stream()
					.filter(pl -> !GTS.getInstance().getIgnorers().contains(pl.getUniqueId()))
					.collect(Collectors.toSet());
			List<Text> broadcast;
			Map<String, Object> variables = Maps.newHashMap();
			variables.put("dummy", listing.getEntry().getEntry());
			variables.put("dummy2", listing);
			variables.put("dummy3", listing.getEntry());

			try {
				broadcast = GTS.getInstance().getTextParsingUtils().parse(
						Lists.newArrayList(
								"{{gts_prefix}} &7A &a{{listing_specifics}} &7has been added to the GTS for &a{{price}}&7!"
						),
						null,
						null,
						variables
				);
			} catch (NucleusException e) {
				broadcast = Lists.newArrayList();
			}
			for(Player pl : players) {
				pl.sendMessages(broadcast);
			}

			GTS.getInstance().getDiscordNotifier().ifPresent(notifier -> {
				Message message = notifier.forgeMessage(GTS.getInstance().getConfig().get(ConfigKeys.DISCORD_NEW_LISTING), "&7A &a{{listing_specifics}} &7has been added to the GTS for &a{{price}}&7!");
				notifier.sendMessage(message);
			});
		}
	}

    public static void addToMarket(Player player, Listing listing) {
	    if(ListingUtils.hasMax(player)) {
		    Map<String, Function<CommandSource, Optional<Text>>> replacements = Maps.newHashMap();
		    replacements.put("max_listings", s -> Optional.of(Text.of(GTS.getInstance().getConfig().get(ConfigKeys.MAX_LISTINGS))));
		    try {
			    player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
					    GTS.getInstance().getMsgConfig().get(MsgConfigKeys.MAX_LISTINGS),
					    player,
					    replacements,
					    null
			    ));
		    } catch (NucleusException e) {
			    e.printStackTrace();
		    }
		    return;
	    }

	    ListEvent listEvent = new ListEvent(
			    player,
			    listing,
			    Sponge.getCauseStackManager().getCurrentCause()
	    );
	    Sponge.getEventManager().post(listEvent);

	    if(!listEvent.isCancelled()) {
	    	// We can check here for minimum prices, if we decide to support it

		    Map<String, Object> variables = Maps.newHashMap();
		    variables.put("dummy", listing.getEntry().getEntry());
		    variables.put("dummy2", listing);
		    variables.put("dummy3", listing.getEntry());

		    if(GTS.getInstance().getConfig().get(ConfigKeys.MIN_PRICING_ENABLED) && listing.getEntry() instanceof Minable) {
		    	MoneyPrice price = (MoneyPrice) listing.getEntry().getPrice();
		    	try {
				    MoneyPrice min = ((Minable) listing.getEntry()).calcMinPrice();
				    if (price.getPrice().compareTo(min.getPrice()) < 0) {
					    Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
					    tokens.put("min_price", src -> Optional.of(min.getText()));
					    try {
						    player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
								    GTS.getInstance().getMsgConfig().get(MsgConfigKeys.MIN_PRICE_ERROR),
								    player,
								    tokens,
								    variables
						    ));
					    } catch (NucleusException e) {
						    player.sendMessage(Text.of(
								    GTSInfo.ERROR, TextColors.GRAY, "To sell your ", TextColors.YELLOW, listing.getEntry().getName(),
								    TextColors.GRAY, "you must list it for ", TextColors.GREEN, min.getText()
						    ));
					    }

					    return;
				    }
			    } catch (PricingException e) {
		    		GTS.getInstance().getConsole().ifPresent(console -> console.sendMessage(Text.of(
		    				GTSInfo.ERROR, e.getMessage()
				    )));
				    return;
			    }
		    }

		    Optional<BigDecimal> tax = Optional.empty();
		    if(GTS.getInstance().getConfig().get(ConfigKeys.TAX_ENABLED)) {
			    try {
			    	BigDecimal t = listing.getEntry().getPrice().calcTax(player);
				    if(t.signum() == -1) {
						Map<String, Function<CommandSource, Optional<Text>>> extras = Maps.newHashMap();
						extras.put("tax", src -> Optional.of(Text.of(t)));

						player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
								GTS.getInstance().getMsgConfig().get(MsgConfigKeys.TAX_INVALID),
								player,
								extras,
								variables
						));
						return;
				    } else {
				    	tax = Optional.of(t);
				    }
			    } catch (Exception e) {
			    	if(e instanceof PricingException) {
					    MessageUtils.genAndSendErrorMessage(
							    "Tax Error",
							    "Unable to calculate tax",
							    "Player: " + player.getName()
					    );
				    }
			    }

			    try {
				    final BigDecimal t = tax.orElse(BigDecimal.ZERO);
				    Map<String, Function<CommandSource, Optional<Text>>> extras = Maps.newHashMap();
				    extras.put("tax", src -> Optional.of(
						    Text.of(GTS.getInstance().getEconomy().getDefaultCurrency().format(t))));
				    player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
						    GTS.getInstance().getMsgConfig().get(MsgConfigKeys.TAX_APPLICATION),
						    player,
						    extras,
						    null
				    ));
				    GTS.getInstance().getEconomy().getOrCreateAccount(player.getUniqueId()).ifPresent(acc -> {
				    	acc.withdraw(GTS.getInstance().getEconomy().getDefaultCurrency(), t, Sponge.getCauseStackManager().getCurrentCause());
				    });
			    } catch (NucleusException e) {
				    MessageUtils.genAndSendErrorMessage(
						    "Message Parse Error",
						    "Nucleus was unable to decode a message properly...",
						    "Template: " + GTS.getInstance().getMsgConfig().get(MsgConfigKeys.TAX_APPLICATION)
				    );
			    }
		    }

		    if(!listing.getEntry().doTakeAway(player)) {
			    // Refund applied tax
			    if(GTS.getInstance().getConfig().get(ConfigKeys.TAX_ENABLED)) {
				    try {
					    UniqueAccount acc = GTS.getInstance().getEconomy().getOrCreateAccount(
							    player.getUniqueId()).orElse(null);
					    if (acc != null)
						    acc.deposit(GTS.getInstance().getEconomy().getDefaultCurrency(),
						                tax.orElse(BigDecimal.ZERO),
						                Cause.builder().append(GTS.getInstance()).build(EventContext.empty()));

					    player.sendMessage(Text.of(GTSInfo.ERROR, TextColors.RED,
					                               "Your listing failed to be taken, so we have refunded the tax applied!"));
				    } catch (Exception e) {
					    e.printStackTrace();
				    }
			    }
			    return;
		    }

			try {
				player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
						GTS.getInstance().getMsgConfig().get(MsgConfigKeys.ADD_TEMPLATE),
						player,
						null,
						variables
				));
			} catch (NucleusException e) {
				MessageUtils.genAndSendErrorMessage(
						"Message Parse Error",
						"Nucleus was unable to decode a message properly...",
						"Template: " + GTS.getInstance().getMsgConfig().get(MsgConfigKeys.ADD_TEMPLATE)
				);
			}

		    GTS.getInstance().getStorage().addListing(listing);
			GTS.getInstance().getListingsCache().add(listing);

			// Broadcast a message to everyone but the player who deposited the listing and the ignorers
		    Set<Player> players = Sponge.getServer().getOnlinePlayers().stream()
				    .filter(pl -> !pl.getUniqueId().equals(player.getUniqueId()))
				    .filter(pl -> !GTS.getInstance().getIgnorers().contains(pl.getUniqueId()))
				    .collect(Collectors.toSet());
			List<Text> broadcast;

		    try {
			    broadcast = GTS.getInstance().getTextParsingUtils().parse(
			    		GTS.getInstance().getMsgConfig().get(MsgConfigKeys.ADD_BROADCAST),
					    player,
					    null,
					    variables
			    );
		    } catch (NucleusException e) {
			    broadcast = Lists.newArrayList(
			    		Text.of(GTSInfo.PREFIX, "&e" + player.getName() + " &7has deposited a new listing!")
			    );
		    }
		    for(Player pl : players) {
			    pl.sendMessages(broadcast);
		    }

		    final Text b;
		    try {
			    b = GTS.getInstance().getTextParsingUtils().parse(
					    "&c{{player}} &7has added a &a{{listing_specifics}} &7to the GTS for &a{{price}}&7!",
					    player,
					    null,
					    variables
			    );
			    GTS.getInstance().getDiscordNotifier().ifPresent(notifier -> {
				    Message message = notifier.forgeMessage(GTS.getInstance().getConfig().get(ConfigKeys.DISCORD_NEW_LISTING), b.toPlain());
				    notifier.sendMessage(message);
			    });
		    } catch (NucleusException e) {
			    e.printStackTrace();
		    }


		    Log add = Log.builder()
				    .action(LogAction.Addition)
				    .source(player.getUniqueId())
				    .hover(Log.forgeTemplate(player, listing, LogAction.Addition))
				    .build();
		    GTS.getInstance().getStorage().addLog(add);

		    //GTS.getInstance().getUpdater().sendUpdate();
	    }
    }

    public static void purchase(Player player, Listing listing) {
	    Map<String, Object> variables = Maps.newHashMap();
	    variables.put("dummy", listing.getEntry().getEntry());
	    variables.put("dummy2", listing);
	    variables.put("dummy3", listing.getEntry());

		if(!GTS.getInstance().getListingsCache().contains(listing)) {
			try {
				player.sendMessages(
						GTS.getInstance().getTextParsingUtils().parse(
								GTS.getInstance().getMsgConfig().get(MsgConfigKeys.ALREADY_CLAIMED),
								player,
								null,
								variables
						)
				);
			} catch (NucleusException e) {
				e.printStackTrace();
			}
			return;
		}

		if(listing.hasExpired()) {
			player.sendMessage(Text.of(GTSInfo.ERROR, TextColors.GRAY, "That listing has expired..."));
			return;
		}

		Price price = listing.getEntry().getPrice();
	    try {
		    if(price.canPay(player)) {

				PurchaseEvent purchaseEvent = new PurchaseEvent(
						player,
						listing,
						Sponge.getCauseStackManager().getCurrentCause()
				);
				if (Sponge.getEventManager().post(purchaseEvent)) {
					return;
				}

		    	if(!listing.getEntry().giveEntry(player)) {
		    	    return;
			    }

				price.pay(player);
				player.sendMessages(
						GTS.getInstance().getTextParsingUtils().parse(
								GTS.getInstance().getMsgConfig().get(MsgConfigKeys.PURCHASE_PAY),
								player,
								null,
								variables
						)
				);

				if(!price.supportsOfflineReward()) {
					Player receiver;
					if ((receiver = Sponge.getServer().getPlayer(listing.getOwnerUUID()).orElse(null)) != null) {
						price.reward(listing.getOwnerUUID());
						receiver.sendMessages(
								GTS.getInstance().getTextParsingUtils().parse(
										GTS.getInstance().getMsgConfig().get(MsgConfigKeys.PURCHASE_RECEIVE),
										player,
										null,
										variables
								)
						);
					}
					else {
						addHeldPrice(new PriceHolder(UUID.randomUUID(), listing.getOwnerUUID(), price));
					}
				} else {
					price.reward(listing.getOwnerUUID());
					Sponge.getServer().getPlayer(listing.getOwnerUUID()).ifPresent(pl -> {
						try {
							pl.sendMessages(
									GTS.getInstance().getTextParsingUtils().parse(
											GTS.getInstance().getMsgConfig().get(MsgConfigKeys.PURCHASE_RECEIVE),
											player,
											null,
											variables
									)
							);
						} catch (NucleusException e) {
							e.printStackTrace();
						}
					});
				}

				deleteEntry(listing);

			    final String b = GTS.getInstance().getTextParsingUtils().parse(
					    "{{buyer}} just purchased a {{listing_specifics}} from {{seller}} for {{price}}!",
					    player,
					    null,
					    variables
			    ).toPlain();
			    GTS.getInstance().getDiscordNotifier().ifPresent(notifier -> {
				    Message message = notifier.forgeMessage(GTS.getInstance().getConfig().get(ConfigKeys.DISCORD_SELL_LISTING), b);
				    notifier.sendMessage(message);
			    });

			    Log buyer = Log.builder()
					    .action(LogAction.Purchase)
					    .source(player.getUniqueId())
					    .hover(Log.forgeTemplate(player, listing, LogAction.Purchase))
					    .build();
			    GTS.getInstance().getStorage().addLog(buyer);

			    Log seller = Log.builder()
					    .action(LogAction.Sell)
					    .source(listing.getOwnerUUID())
					    .hover(Log.forgeTemplate(player, listing, LogAction.Sell))
					    .build();
			    GTS.getInstance().getStorage().addLog(seller);

				//GTS.getInstance().getUpdater().sendUpdate();
		    } else {
		    	player.sendMessages(
		    			GTS.getInstance().getTextParsingUtils().parse(
		    					GTS.getInstance().getMsgConfig().get(MsgConfigKeys.NOT_ENOUGH_FUNDS),
							    player,
							    null,
							    variables
					    )
			    );
		    }
	    } catch (Exception e) {
	    	if(e instanceof RewardException) {
			    addHeldPrice(new PriceHolder(UUID.randomUUID(), listing.getOwnerUUID(), price));
		    } else {
			    player.sendMessages(
					    Text.of(GTSInfo.ERROR, "Unfortunately, you were unable to purchase the listing due to an error...")
			    );
			    GTS.getInstance().getConsole().ifPresent(console -> console.sendMessages(
					    Text.of(GTSInfo.ERROR, e.getMessage())
			    ));
		    }
	    }
    }

    public static void bid(Player player, Listing listing) {
	    Map<String, Object> variables = Maps.newHashMap();
	    variables.put("listing_specifics", listing);
	    variables.put("listing_name", listing);
	    variables.put("time_left", listing);
	    variables.put("id", listing);

		if(listing.getAucData() != null) {
			if(listing.getAucData().getHighBidder() != null && listing.getAucData().getHighBidder().equals(player.getUniqueId())) {
				try {
					player.sendMessages(
							GTS.getInstance().getTextParsingUtils().parse(
									GTS.getInstance().getMsgConfig().get(MsgConfigKeys.AUCTION_IS_HIGH_BIDDER),
									player,
									null,
									variables
							)
					);
				} catch (NucleusException e) {
					e.printStackTrace();
				}
			} else {
				MoneyPrice newPrice;
				try {
					newPrice = ((MoneyPrice)listing.getEntry().getPrice()).calculate(listing.getAucData().getIncrement());
					if(!newPrice.canPay(player)) {
						player.sendMessage(Text.of(GTSInfo.ERROR, "Your balance is too low to bid..."));
						return;
					}
				} catch (PricingException e) {
					return;
				}

				UUID oldHigh = listing.getAucData().getHighBidder();
				Sponge.getServer().getPlayer(oldHigh).ifPresent(p -> {
					Text message = Text.of(GTSInfo.PREFIX, TextColors.GRAY, TextActions.executeCallback(src -> bid((Player) src, listing)), TextActions.showText(Text.of(TextColors.GRAY, "Click to bid!")), "You've been ", TextColors.RED, "outbid", TextColors.GRAY, "... Click here to bid once more!");
					p.sendMessages(message);
				});

				listing.getAucData().setHighBidder(player.getUniqueId());
				listing.getAucData().setHbName(Text.of(player.getName()));
				listing.getAucData().setHbNameString(player.getName());
				if(listing.getExpiration().getTime() / 1000 - Date.from(Instant.now()).getTime() / 1000 < 15) {
					listing.increaseTimeForBid();
				}
				try {
					player.sendMessages(GTS.getInstance().getTextParsingUtils().parse(
							GTS.getInstance().getMsgConfig().get(MsgConfigKeys.AUCTION_BID),
							player,
							null,
							variables
					));
				} catch (NucleusException e) {
					GTS.getInstance().getConsole().ifPresent(console -> {
						console.sendMessage(Text.of(
								GTSInfo.ERROR, "Unable to send a bid message for ", player.getName()
						));
					});
				}
				try {
					((MoneyPrice)listing.getEntry().getPrice()).add(listing.getAucData().getIncrement());
				} catch (PricingException e) {
					e.printStackTrace();
				}
				GTS.getInstance().getStorage().updateListing(listing);

				try {
					List<Text> broadcast = GTS.getInstance().getTextParsingUtils().parse(
							GTS.getInstance().getMsgConfig().get(MsgConfigKeys.AUCTION_BID_BROADCAST),
							player,
							null,
							variables
					);
					Sponge.getServer().getOnlinePlayers().stream()
							.filter(pl -> GTS.getInstance().getIgnorers().contains(pl.getUniqueId()))
							.forEach(pl -> pl.sendMessages(broadcast));

				} catch (NucleusException e) {
					e.printStackTrace();
				}
			}
		}
    }

    private static boolean hasMax(User user) {
        return hasMax(user.getUniqueId());
    }

    private static boolean hasMax(UUID uuid) {
        int count = GTS.getInstance().getListingsCache().stream().filter(listing -> listing.getOwnerUUID().equals(uuid)).collect(Collectors.toList()).size();
        return count >= GTS.getInstance().getConfig().get(ConfigKeys.MAX_LISTINGS);
    }

	public static void deleteEntry(Listing entry) {
    	GTS.getInstance().getListingsCache().remove(entry);
    	GTS.getInstance().getStorage().removeListing(entry.getUuid());
	}

	public static void addHeldEntry(EntryHolder holder) {
		GTS.getInstance().getHeldEntryCache().add(holder);
		GTS.getInstance().getStorage().addHeldElement(holder);
	}

	public static void addHeldPrice(PriceHolder holder) {
		GTS.getInstance().getHeldPriceCache().add(holder);
		GTS.getInstance().getStorage().addHeldPrice(holder);
	}
}
