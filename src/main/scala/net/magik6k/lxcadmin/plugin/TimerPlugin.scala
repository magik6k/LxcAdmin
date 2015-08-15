package net.magik6k.lxcadmin.plugin

import net.magik6k.jwwf.core.{JwwfServer, JwwfPlugin}
import net.magik6k.jwwf.core.plugin.IPluginGlobal

object TimerPlugin extends JwwfPlugin with IPluginGlobal {
	override def onAttach(server: JwwfServer): Unit = {
		//Totally not crappiest/hackiest timer ever
		server.getCreator().appendHead("<script>var oldget=global[\"JWWF-storageGet\"];\nglobal[\"JWWF-storageGet\"] = function(data){setTimeout(function(){oldget(data)}, 500)}</script>")
	}
}
