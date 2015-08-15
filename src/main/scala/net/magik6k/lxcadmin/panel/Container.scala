package net.magik6k.lxcadmin.panel

import net.magik6k.jwwf.enums.Type
import net.magik6k.jwwf.widgets.basic.{TextLabel, ProgressBar}
import net.magik6k.jwwf.widgets.basic.panel.{NamedPanel, Row}
import net.magik6k.lxcadmin.widget.MemMonProvider

import scala.io.Source

class Container(val name: String) extends Row(2) {

	// CPU

	val cpuBars = Source.fromFile("/sys/fs/cgroup/cpu/lxc/"+name+"/cpuacct.usage_percpu").mkString.split("\\s+").map(u => (new ProgressBar(0), u.toLong))

	val barRow = new Row(cpuBars.length)
	cpuBars.foreach{case(bar, usage) => barRow.put(bar)}

	this.put(new NamedPanel(barRow, "CPU Usage", Type.SUCCESS))

	// MEMORY

	val memoryLimit = math.min(MemMonProvider.total * 1024, Source.fromFile("/sys/fs/cgroup/memory/lxc/magik6k/memory.limit_in_bytes").getLines().map(_.toLong).find(_=>true).get)
	var memoryUsed = Source.fromFile("/sys/fs/cgroup/memory/lxc/magik6k/memory.usage_in_bytes").getLines().map(_.toLong).find(_=>true).get

	val memoryBar = new ProgressBar(Type.SUCCESS, memoryUsed / memoryLimit.toDouble)
	val memoryLabel = new TextLabel("RAM (0%) 0M")
	this.put(new NamedPanel(new Row(2, memoryLabel.asPanel(3) , memoryBar.asPanel(9) ), "Memory", Type.WARNING))

	// REFRESHING

	var lastTime = System.nanoTime()

	def refresh() {
		val time = System.nanoTime()

		//CPU

		Source.fromFile("/sys/fs/cgroup/cpu/lxc/magik6k/cpuacct.usage_percpu").mkString.split("\\s+")
			.zipWithIndex.foreach{case (usage, id) =>
			val (bar, lastUse) = cpuBars(id)
			val use = usage.toLong
			bar.setProgress((use - lastUse) / (time - lastTime.toDouble))
			cpuBars(id) = (bar, use)
		}

		// MEMORY

		memoryUsed = Source.fromFile("/sys/fs/cgroup/memory/lxc/magik6k/memory.usage_in_bytes").getLines().map(_.toLong).find(_=>true).get
		memoryBar.setProgress(memoryUsed / memoryLimit.toDouble)
		memoryLabel.setText("RAM (" + math.floor((memoryUsed / memoryLimit.toDouble)*100).toInt + "%) " + (memoryUsed / 1024 / 1024) + "M")

		lastTime = time
	}
}
