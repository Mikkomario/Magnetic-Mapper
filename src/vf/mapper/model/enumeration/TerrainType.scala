package vf.mapper.model.enumeration

/**
 * An enumeration for different types of terrain
 * @author Mikko Hilpinen
 * @since 22.1.2023, v0.1
 */
sealed trait TerrainType

object TerrainType
{
	/**
	 * Represents icy terrain
	 */
	case object Snow extends TerrainType
	/**
	 * Represents all water terrain (oceans, lakes, rivers)
	 */
	case object Water extends TerrainType
	/**
	 * Represents all land terrain (coast, desert, forest, mountains, etc.)
	 */
	case object Land extends TerrainType
}
