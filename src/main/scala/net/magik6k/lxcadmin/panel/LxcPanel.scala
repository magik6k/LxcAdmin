package net.magik6k.lxcadmin.panel

import java.io.File

import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.util.Tab
import net.magik6k.jwwf.widgets.basic.TextLabel
import net.magik6k.jwwf.widgets.basic.panel.{TabbedPanel, NamedPanel, Panel, Row}

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

	var row = new Row(2)

	row.put(new NamedPanel(new TextLabel(base.mkString(", ")), "Base systems").asPanel(12))

	val containerList = systems.map(file => new Container(file.getName))

	var containers = new TabbedPanel(systems.length,
		containerList.map(container => new Tab(new NamedPanel(container, "Container <b>"+container.name+"</b>", Type.INFO), container.name, Type.SUCCESS)).toArray:_*)

	row.put(containers.asPanel(12))



	put(row)

	def refresh() {
		containerList.foreach(_.refresh())
	}
}
