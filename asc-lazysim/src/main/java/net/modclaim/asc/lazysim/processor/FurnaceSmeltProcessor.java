package net.modclaim.asc.lazysim.processor;

import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import net.modclaim.asc.lazysim.math.SmeltingCalculator;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Simulates furnace smelting and fuel consumption over elapsed chunk hibernation duration.
 */
public final class FurnaceSmeltProcessor implements LazySimulatable {

    @Override
    @NotNull
    public String getId() {
        return "furnaces";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    @NotNull
    public CatchUpResult onCatchUp(@NotNull CatchUpContext context) {
        long start = System.nanoTime();
        Chunk chunk = context.getChunk();
        long elapsedTicks = context.getElapsed().toSeconds() * 20L;
        if (elapsedTicks <= 0) {
            return CatchUpResult.empty(getId());
        }

        int blocksUpdated = 0;

        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Furnace furnace)) continue;

            FurnaceInventory inv = furnace.getInventory();
            ItemStack smelting = inv.getSmelting();
            ItemStack fuel = inv.getFuel();
            ItemStack result = inv.getResult();

            if (smelting == null || smelting.getType().isAir()) continue;

            int cookTotal = furnace.getCookTimeTotal();
            if (cookTotal <= 0) {
                cookTotal = (furnace.getType() == Material.BLAST_FURNACE || furnace.getType() == Material.SMOKER) ? 100 : 200;
            }

            int fuelValue = getFuelBurnTime(fuel);
            int sourceCount = smelting.getAmount();
            int fuelCount = (fuel != null) ? fuel.getAmount() : 0;
            int currentResultCount = (result != null) ? result.getAmount() : 0;
            int maxStack = (result != null) ? result.getMaxStackSize() : 64;

            SmeltingCalculator.Result calc = SmeltingCalculator.calculate(
                    furnace.getCookTime(),
                    furnace.getBurnTime(),
                    cookTotal,
                    fuelValue,
                    sourceCount,
                    fuelCount,
                    currentResultCount,
                    maxStack,
                    elapsedTicks
            );

            if (calc.itemsSmelted > 0 || calc.fuelItemsConsumed > 0) {
                // Update source stack
                int newSourceCount = sourceCount - calc.itemsSmelted;
                if (newSourceCount <= 0) {
                    inv.setSmelting(null);
                } else {
                    smelting.setAmount(newSourceCount);
                    inv.setSmelting(smelting);
                }

                // Update fuel stack
                if (fuel != null && calc.fuelItemsConsumed > 0) {
                    int newFuelCount = fuelCount - calc.fuelItemsConsumed;
                    if (newFuelCount <= 0) {
                        inv.setFuel(null);
                    } else {
                        fuel.setAmount(newFuelCount);
                        inv.setFuel(fuel);
                    }
                }

                // Update result stack
                Material resultMat = getSmeltResult(smelting.getType());
                if (resultMat != null) {
                    if (result == null || result.getType().isAir()) {
                        inv.setResult(new ItemStack(resultMat, calc.itemsSmelted));
                    } else {
                        result.setAmount(result.getAmount() + calc.itemsSmelted);
                        inv.setResult(result);
                    }
                }

                furnace.setCookTime((short) calc.remainingCookTime);
                furnace.setBurnTime((short) calc.remainingBurnTime);
                furnace.update(true);
                blocksUpdated++;
            }
        }

        long duration = System.nanoTime() - start;
        return new CatchUpResult(getId(), 0, blocksUpdated, duration, true, "Furnaces processed: " + blocksUpdated);
    }

    private int getFuelBurnTime(ItemStack fuel) {
        if (fuel == null) return 0;
        Material m = fuel.getType();
        if (m == Material.COAL || m == Material.CHARCOAL) return 1600;
        if (m == Material.BLAZE_ROD) return 2400;
        if (m == Material.LAVA_BUCKET) return 20000;
        if (m.name().contains("PLANK") || m.name().contains("LOG")) return 300;
        if (m == Material.STICK) return 100;
        return 200;
    }

    private Material getSmeltResult(Material source) {
        if (source == Material.RAW_IRON) return Material.IRON_INGOT;
        if (source == Material.RAW_GOLD) return Material.GOLD_INGOT;
        if (source == Material.RAW_COPPER) return Material.COPPER_INGOT;
        if (source == Material.COBBLESTONE) return Material.STONE;
        if (source == Material.SAND) return Material.GLASS;
        if (source == Material.BEEF) return Material.COOKED_BEEF;
        if (source == Material.PORKCHOP) return Material.COOKED_PORKCHOP;
        if (source == Material.CHICKEN) return Material.COOKED_CHICKEN;
        if (source == Material.MUTTON) return Material.COOKED_MUTTON;
        if (source == Material.POTATO) return Material.BAKED_POTATO;
        if (source == Material.KELP) return Material.DRIED_KELP;
        return null;
    }

    @Override
    public void onHibernate(@NotNull Chunk chunk, long epochMs) {
        // No-op
    }
}
