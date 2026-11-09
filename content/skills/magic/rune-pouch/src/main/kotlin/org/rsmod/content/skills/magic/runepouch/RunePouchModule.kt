package org.rsmod.content.skills.magic.runepouch

import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.plugin.module.PluginModule

public class RunePouchModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerItemStorageHook>(RunePouchStorageHook::class.java)
    }
}
