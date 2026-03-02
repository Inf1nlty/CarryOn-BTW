package tschipp.carryon.client.render;

public interface ICarrying {

    public boolean carryOn$isCarryingBlock();
    public boolean carryOn$isCarryingEntity();

    public void carryOn$setCarryingBlock(boolean isCarrying);
    public void carryOn$setCarryingEntity(boolean isCarrying);

}