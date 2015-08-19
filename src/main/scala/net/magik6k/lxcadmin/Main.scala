package net.magik6k.lxcadmin

import java.lang.management.ManagementFactory

import net.magik6k.jwwf.core.JwwfServer
import net.magik6k.lxcadmin.plugin.TimerPlugin
import net.magik6k.lxcadmin.widget.{MemMonProvider, CPUMonProvider}

import scala.util.Try

object Main {
	def main(args: Array[String]) {
		println(ManagementFactory.getRuntimeMXBean.getName)

		new Thread(new Runnable {
			override def run(): Unit = {
				while(true) {
					CPUMonProvider.refresh()
					MemMonProvider.refresh()
					Thread.sleep(500)
				}
			}
		}).start()

		val server = new JwwfServer(if (args.length > 0) args(0).toInt else 8888)
		Try { if (args.length > 1) server.setApiUrl(args(1)) }
		server.attachPlugin(TimerPlugin)
		server.bindWebapp(classOf[Client])
		server.start()
	}
}
