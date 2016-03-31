package net.magik6k.lxcadmin

import net.magik6k.jwwf.core.{MainFrame, User}
import net.magik6k.lxcadmin.panel.MainPanel

class Client extends User {
	override def initializeUser(rootFrame: MainFrame) {
		try {
			rootFrame.setTitle("LXC Admin")
			rootFrame.put(new MainPanel(this))
		} catch {
			case e: Exception => e.printStackTrace()
		}
	}
}
