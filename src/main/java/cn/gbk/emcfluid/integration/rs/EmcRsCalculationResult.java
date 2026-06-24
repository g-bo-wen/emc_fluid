package cn.gbk.emcfluid.integration.rs;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.preview.ICraftingPreviewElement;
import com.refinedmods.refinedstorage.api.autocrafting.task.CalculationResultType;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICalculationResult;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmcRsCalculationResult implements ICalculationResult {
    private final CalculationResultType type;
    private final List<ICraftingPreviewElement> previewElements;
    private final ICraftingTask task;

    public EmcRsCalculationResult(CalculationResultType type, @Nullable ICraftingTask task) {
        this(type, List.of(), task);
    }

    public EmcRsCalculationResult(CalculationResultType type, List<ICraftingPreviewElement> previewElements, @Nullable ICraftingTask task) {
        this.type = type;
        this.previewElements = List.copyOf(previewElements);
        this.task = task;
    }

    @Override
    public CalculationResultType getType() {
        return type;
    }

    @Override
    public List<ICraftingPreviewElement> getPreviewElements() {
        return previewElements;
    }

    @Nullable
    @Override
    public ICraftingTask getTask() {
        return task;
    }

    @Override
    public boolean isOk() {
        return type == CalculationResultType.OK;
    }

    @Nullable
    @Override
    public ICraftingPattern getRecursedPattern() {
        return null;
    }
}
