package net.jaxx0rr.commandblockscanner;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.World;

public class CommandBlockScanner implements ModInitializer {
	public static final String MOD_ID = "commandblockscanner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("scanCB")
				.then(CommandManager.argument("range in blocks", IntegerArgumentType.integer())
						.then(CommandManager.argument("command text to search - (use \"all\" to show all)", StringArgumentType.string())
								.executes(CommandBlockScanner::scan)
						))));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("replaceCB")
				.then(CommandManager.argument("range in blocks", IntegerArgumentType.integer())
						.then(CommandManager.argument("command text to replace", StringArgumentType.string())
								.then(CommandManager.argument("command text to replace with", StringArgumentType.string())
										.executes(CommandBlockScanner::replace)
								)))));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("clearCB")
				.executes(CommandBlockScanner::clear)
		));

	}

	private static int clear(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer(); //MinecraftClient.getInstance().player;

		if (player != null) {
			if(!player.getAbilities().creativeMode) {
				context.getSource().sendFeedback(() -> Text.literal("Must be in creative mode!"), false);
			} else {
				if (player.getServer() != null) {
					CommandManager commandManager = player.getServer().getCommandManager();
					ServerCommandSource commandSource = player.getServer().getCommandSource();
					String ccmd = "kill @e[type=text_display]";
					commandManager.executeWithPrefix(commandSource, ccmd);
				}
			}
		}
		return 1;
	}

	private static int scan(CommandContext<ServerCommandSource> context) {
		int range = IntegerArgumentType.getInteger(context, "range in blocks");
		String search = StringArgumentType.getString(context, "command text to search - (use \"all\" to show all)");
		ServerCommandSource source = context.getSource();
		//World world = source.getWorld();

		ServerPlayerEntity player = source.getPlayer(); //MinecraftClient.getInstance().player;

		if (player != null) {
			MinecraftServer server = player.getServer();
			if (server != null) {
				if (range < 1 || range > 300) {
					context.getSource().sendFeedback(() -> Text.literal("Parameter range must be between 1 and 300"), false);
				} else if (!player.getAbilities().creativeMode) {
					context.getSource().sendFeedback(() -> Text.literal("Must be in creative mode!"), false);
				} else {

					World world = player.getWorld();

					int x = (int) player.getX(); //-133
					int y = (int) player.getY(); //63
					int z = (int) player.getZ(); //-65

					context.getSource().sendFeedback(() -> Text.literal("--- Starting scan at player pos: " + x + " " + y + " " + z), false);
					if (range > 100)
						context.getSource().sendFeedback(() -> Text.literal("WARNING: Using a value too high might crash the server!"), false);

					CommandManager commandManager = server.getCommandManager();
					ServerCommandSource commandSource = server.getCommandSource();
					String ccmd = "kill @e[type=text_display]";
					commandManager.executeWithPrefix(commandSource, ccmd);

					var cBlocks = 0;

					for (int cx = -range; cx <= range; cx++) {
						for (int cy = -range; cy <= range; cy++) {
							for (int cz = -range; cz <= range; cz++) {

								int fx = cx + x;
								int fy = cy + y;
								int fz = cz + z;

								String tcmd = "summon minecraft:text_display " + fx + " " + fy + " " + fz + " {text:'{\"text\":\"FOUND\"}',text_opacity:255,color:\"yellow\",billboard:\"center\",see_through:true}";

								//Block b = world.getBlockState(new BlockPos(fx,fy,fz)).getBlock();
								//context.getSource().sendFeedback(() -> Text.literal("Block at: "+ fx + " " +fy + " " + fz + " is: "+b.getName()), false);

								BlockEntity be = world.getBlockEntity(new BlockPos(fx, fy, fz));
								if (be != null) {
									//context.getSource().sendFeedback(() -> Text.literal(""+be.getType()), false);
									if (be.getType() == BlockEntityType.COMMAND_BLOCK) {

										CommandBlockBlockEntity bec = (CommandBlockBlockEntity) be;
										CommandBlockExecutor commandExecutor = bec.getCommandExecutor();
										String command = commandExecutor.getCommand();

										if (search.equalsIgnoreCase("all")) {
											commandManager.executeWithPrefix(commandSource, tcmd);
											context.getSource().sendFeedback(() -> Text.literal("Found CB at: " + fx + " " + fy + " " + fz + " with command: " + command), false);
											cBlocks++;
										} else
										if (command.contains(search)) {
											commandManager.executeWithPrefix(commandSource, tcmd);
											context.getSource().sendFeedback(() -> Text.literal("Found CB at: " + fx + " " + fy + " " + fz + " with command: " + command), false);
											cBlocks++;
										}
									}
								}
							}
						}
					}

					int finalCBlock = cBlocks;
					context.getSource().sendFeedback(() -> Text.literal("--- Scan finished! Found " + finalCBlock + " Command Blocks"), false);
				}
			}
		}


		return 1;
	}

	private static int replace(CommandContext<ServerCommandSource> context) {
		int range = IntegerArgumentType.getInteger(context, "range in blocks");
		String search = StringArgumentType.getString(context, "command text to replace");
		String replace = StringArgumentType.getString(context, "command text to replace with");
		ServerCommandSource source = context.getSource();
		//World world = source.getWorld();

		ServerPlayerEntity player = source.getPlayer(); //MinecraftClient.getInstance().player;

		if (player != null) {
			MinecraftServer server = player.getServer();
			if (server != null) {
				if (range < 1 || range > 300) {
					context.getSource().sendFeedback(() -> Text.literal("Parameter range must be between 1 and 300"), false);
				} else if (!player.getAbilities().creativeMode) {
					context.getSource().sendFeedback(() -> Text.literal("Must be in creative mode!"), false);
				} else {

					World world = player.getWorld();

					int x = (int) player.getX(); //-133
					int y = (int) player.getY(); //63
					int z = (int) player.getZ(); //-65

					context.getSource().sendFeedback(() -> Text.literal("--- Starting scan at player pos: " + x + " " + y + " " + z), false);
					if (range > 100)
						context.getSource().sendFeedback(() -> Text.literal("WARNING: Using a value too high might crash the server!"), false);

					CommandManager commandManager = server.getCommandManager();
					ServerCommandSource commandSource = server.getCommandSource();
					String ccmd = "kill @e[type=text_display]";
					commandManager.executeWithPrefix(commandSource, ccmd);

					var cBlocks = 0;

					for (int cx = -range; cx <= range; cx++) {
						for (int cy = -range; cy <= range; cy++) {
							for (int cz = -range; cz <= range; cz++) {

								int fx = cx + x;
								int fy = cy + y;
								int fz = cz + z;

								String tcmd = "summon minecraft:text_display " + fx + " " + fy + " " + fz + " {text:'{\"text\":\"REPLACED\"}',text_opacity:255,color:\"yellow\",billboard:\"center\",see_through:true}";

								//Block b = world.getBlockState(new BlockPos(fx,fy,fz)).getBlock();
								//context.getSource().sendFeedback(() -> Text.literal("Block at: "+ fx + " " +fy + " " + fz + " is: "+b.getName()), false);

								BlockEntity be = world.getBlockEntity(new BlockPos(fx, fy, fz));
								if (be != null) {
									//context.getSource().sendFeedback(() -> Text.literal(""+be.getType()), false);
									if (be.getType() == BlockEntityType.COMMAND_BLOCK) {

										CommandBlockBlockEntity bec = (CommandBlockBlockEntity) be;
										CommandBlockExecutor commandExecutor = bec.getCommandExecutor();
										String command = commandExecutor.getCommand();

										if (command.contains(search)) {
											commandManager.executeWithPrefix(commandSource, tcmd);
											String newCommand = command.replace(search, replace);
											commandExecutor.setCommand(newCommand);
											context.getSource().sendFeedback(() -> Text.literal("Replaced CB at: " + fx + " " + fy + " " + fz + " with command: " + newCommand), false);
											cBlocks++;
										}
									}
								}
							}
						}
					}

					int finalCBlock = cBlocks;
					context.getSource().sendFeedback(() -> Text.literal("--- Scan finished! Replaced " + finalCBlock + " Command Blocks"), false);
				}
			}
		}

		return 1;
	}

}