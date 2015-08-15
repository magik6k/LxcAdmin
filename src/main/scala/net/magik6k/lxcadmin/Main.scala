package net.magik6k.lxcadmin

import net.magik6k.jwwf.core.JwwfServer
import net.magik6k.lxcadmin.plugin.TimerPlugin
import net.magik6k.lxcadmin.widget.{MemMonProvider, CPUMonProvider}

object Main {
	def main(args: Array[String]) {
		new Thread(new Runnable {
			override def run(): Unit = {
				while(true) {
					CPUMonProvider.refresh()
					MemMonProvider.refresh()
					Thread.sleep(500)
				}
			}
		}).start()

		val server = new JwwfServer(8888)
		server.attachPlugin(TimerPlugin)
		server.bindWebapp(classOf[Client])
		server.start()
	}
}