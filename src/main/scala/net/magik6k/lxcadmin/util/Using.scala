package net.magik6k.lxcadmin.util


object Using {
	/*def using[T <: { def close() }]
	(resource: T)
		(block: T => Unit)
	{
		try {
			block(resource)
		} finally {
			if (resource != null) resource.close()
		}
	}*/

	def apply[T <: { def close() }, B <: Any]
	(resource: T)
		(block: T => B) : B
	= {
		try {
			block(resource)
		} finally {
			if (resource != null) resource.close()
		}
	}
}
