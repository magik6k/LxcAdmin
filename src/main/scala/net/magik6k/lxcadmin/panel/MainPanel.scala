package net.magik6k.lxcadmin.panel

import java.util.UUID

import net.magik6k.jwwf.core.User
import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.handlers.UserDataHandler
import net.magik6k.jwwf.widgets.basic.panel.{NamedPanel, Row}
import net.magik6k.lxcadmin.widget.{LocalMemMon, LocalCPUMon}

class MainPanel(user: User) extends Row(2) {
	val cpuMon = new LocalCPUMon()
	val memMon = new LocalMemMon()
	val lxcPanel = new LxcPanel()

	val host = new Row(2)
	host.put(new NamedPanel(cpuMon.asPanel(12), "CPU Usage", Type.SUCCESS))
	host.put(new NamedPanel(memMon, "Memory", Type.WARNING))
	this.put(new NamedPanel(host, "<b>Host</b>", Type.DANGER).asPanel(12))
	this.put(lxcPanel)

	//////////
	// TIMER

	var handler: UserDataHandler = null
	handler = new UserDataHandler {
		override def data(key: String, value: String) {
			cpuMon.refresh()
			memMon.refresh()
			lxcPanel.refresh()

			user.getUserData.get(UUID.randomUUID().toString, handler)
		}
	}

	user.getUserData.get(UUID.randomUUID().toString, handler)
}
