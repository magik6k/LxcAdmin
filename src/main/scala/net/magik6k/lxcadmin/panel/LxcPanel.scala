package net.magik6k.lxcadmin.panel

import java.io.File

import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.widgets.basic.TextLabel
import net.magik6k.jwwf.widgets.basic.panel.{NamedPanel, Panel, Row}

class LxcPanel extends Panel(12, 1) {

	val filelist = {
		val lxcDir = new File("/var/lib/lxc")
		if (lxcDir.exists && lxcDir.isDirectory) {
			lxcDir.listFiles.filter(_.isDirectory).toList
		} else {
			List[File]()
		}
	}

	//filelist.foreach(println(_))
	val base = filelist.filter(_.toString.endsWith("base"))
	val systems = filelist.filter(!_.toString.endsWith("base"))

	var containers = new Row(systems.length + 1)

	containers.put(new NamedPanel(new TextLabel(base.mkString(", ")), "Base systems").asPanel(12))

	val containerList = systems.map(file => new Container(file.getName))
	containerList.foreach(container => containers.put(new NamedPanel(container, "Container <b>"+container.name+"</b>", Type.INFO).asPanel(12)))

	put(containers)

	def refresh() {
		containerList.foreach(_.refresh())
	}
}
