package dev.aperture.geometry.recipe.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.recipe.EmitSolidOp;
import dev.aperture.geometry.recipe.GeometryOp;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.SetCutVolumeOp;
import dev.aperture.geometry.recipe.shape.BoxRecipe;
import dev.aperture.geometry.recipe.shape.ExtrudeLinearRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipeConverter;
import dev.aperture.geometry.recipe.shape.SolidShapeRecipe;
import dev.aperture.geometry.recipe.shape.SubtractBoxesRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec2d;
import dev.aperture.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON codec for {@link GeometryRecipe} — NodeCraft graphs, AI output, and data-pack interchange.
 */
public final class GeometryRecipeCodec {
	public static final int VERSION = 1;

	private GeometryRecipeCodec() {
	}

	public static String toJson(GeometryRecipe recipe) {
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		JsonArray ops = new JsonArray();
		for (GeometryOp op : recipe.ops()) {
			ops.add(writeOp(op));
		}
		root.add("ops", ops);
		return root.toString();
	}

	public static GeometryRecipe fromJson(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		int version = root.get("version").getAsInt();
		if (version != VERSION) {
			throw new IllegalArgumentException("unsupported recipe version: " + version);
		}
		List<GeometryOp> ops = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("ops")) {
			ops.add(readOp(element.getAsJsonObject()));
		}
		return new GeometryRecipe(ops);
	}

	public static byte[] toJsonBytes(GeometryRecipe recipe) {
		return toJson(recipe).getBytes(StandardCharsets.UTF_8);
	}

	public static GeometryRecipe fromJsonBytes(byte[] bytes) {
		return fromJson(new String(bytes, StandardCharsets.UTF_8));
	}

	private static JsonObject writeOp(GeometryOp op) {
		return switch (op) {
			case EmitSolidOp emit -> {
				JsonObject json = new JsonObject();
				json.addProperty("type", "emit_solid");
				json.addProperty("componentPath", emit.componentPath());
				json.addProperty("materialSlot", emit.materialSlot());
				json.addProperty("layer", emit.layer().name());
				json.add("shape", writeShape(normalizeShape(emit.shape())));
				json.add("localTransform", writeTransform(emit.localTransform()));
				yield json;
			}
			case SetCutVolumeOp cut -> {
				JsonObject json = new JsonObject();
				json.addProperty("type", "set_cut_volume");
				json.add("cutVolume", writeBounds(cut.cutVolume()));
				yield json;
			}
		};
	}

	private static GeometryOp readOp(JsonObject json) {
		return switch (json.get("type").getAsString()) {
			case "emit_solid" -> new EmitSolidOp(
				json.get("componentPath").getAsString(),
				json.get("materialSlot").getAsString(),
				GeometryLayer.valueOf(json.get("layer").getAsString()),
				readShape(json.getAsJsonObject("shape")),
				readTransform(json.getAsJsonObject("localTransform"))
			);
			case "set_cut_volume" -> new SetCutVolumeOp(readBounds(json.getAsJsonObject("cutVolume")));
			default -> throw new IllegalArgumentException("unknown op type: " + json.get("type").getAsString());
		};
	}

	private static ShapeRecipe normalizeShape(ShapeRecipe recipe) {
		if (recipe instanceof SolidShapeRecipe(var shape)) {
			return ShapeRecipeConverter.fromSolid(shape);
		}
		return recipe;
	}

	private static JsonObject writeShape(ShapeRecipe recipe) {
		return switch (recipe) {
			case BoxRecipe(var bounds) -> {
				JsonObject json = new JsonObject();
				json.addProperty("type", "box");
				json.add("bounds", writeBounds(bounds));
				yield json;
			}
			case ExtrudeLinearRecipe extrude -> {
				JsonObject json = new JsonObject();
				json.addProperty("type", "extrude_linear");
				json.add("profile", writeProfile(extrude.profile()));
				json.add("pathStart", writeVec3(extrude.pathStart()));
				json.add("pathEnd", writeVec3(extrude.pathEnd()));
				json.add("profileU", writeVec3(extrude.profileU()));
				json.add("profileV", writeVec3(extrude.profileV()));
				yield json;
			}
			case SubtractBoxesRecipe subtract -> {
				JsonObject json = new JsonObject();
				json.addProperty("type", "subtract_boxes");
				json.add("base", writeShape(subtract.base()));
				JsonArray boxes = new JsonArray();
				for (BoundingBox box : subtract.subtractBoxes()) {
					boxes.add(writeBounds(box));
				}
				json.add("subtractBoxes", boxes);
				yield json;
			}
			case SolidShapeRecipe ignored -> throw new IllegalArgumentException("cannot serialize opaque SolidShapeRecipe");
		};
	}

	private static ShapeRecipe readShape(JsonObject json) {
		return switch (json.get("type").getAsString()) {
			case "box" -> new BoxRecipe(readBounds(json.getAsJsonObject("bounds")));
			case "extrude_linear" -> new ExtrudeLinearRecipe(
				readProfile(json.getAsJsonObject("profile")),
				readVec3(json.getAsJsonObject("pathStart")),
				readVec3(json.getAsJsonObject("pathEnd")),
				readVec3(json.getAsJsonObject("profileU")),
				readVec3(json.getAsJsonObject("profileV"))
			);
			case "subtract_boxes" -> {
				ShapeRecipe base = readShape(json.getAsJsonObject("base"));
				List<BoundingBox> boxes = new ArrayList<>();
				for (JsonElement element : json.getAsJsonArray("subtractBoxes")) {
					boxes.add(readBounds(element.getAsJsonObject()));
				}
				yield new SubtractBoxesRecipe(base, boxes);
			}
			default -> throw new IllegalArgumentException("unknown shape type: " + json.get("type").getAsString());
		};
	}

	private static JsonObject writeProfile(ProfileCurve profile) {
		JsonObject json = new JsonObject();
		JsonArray points = new JsonArray();
		for (Vec2d point : profile.points()) {
			JsonObject p = new JsonObject();
			p.addProperty("u", point.u());
			p.addProperty("v", point.v());
			points.add(p);
		}
		json.add("points", points);
		return json;
	}

	private static ProfileCurve readProfile(JsonObject json) {
		List<Vec2d> points = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("points")) {
			JsonObject point = element.getAsJsonObject();
			points.add(new Vec2d(point.get("u").getAsDouble(), point.get("v").getAsDouble()));
		}
		return ProfileCurve.fromPoints(points);
	}

	private static JsonObject writeTransform(Transform3d transform) {
		JsonObject json = new JsonObject();
		json.add("origin", writeVec3(transform.origin()));
		json.addProperty("facing", transform.facing().name());
		json.add("rotationAxisOrigin", writeVec3(transform.rotationAxisOrigin()));
		json.add("rotationAxisDirection", writeVec3(transform.rotationAxisDirection()));
		json.addProperty("rotationRadians", transform.rotationRadians());
		return json;
	}

	private static Transform3d readTransform(JsonObject json) {
		return new Transform3d(
			readVec3(json.getAsJsonObject("origin")),
			Facing.valueOf(json.get("facing").getAsString()),
			readVec3(json.getAsJsonObject("rotationAxisOrigin")),
			readVec3(json.getAsJsonObject("rotationAxisDirection")),
			json.get("rotationRadians").getAsDouble()
		);
	}

	private static JsonObject writeBounds(BoundingBox bounds) {
		JsonObject json = new JsonObject();
		json.add("min", writeVec3(bounds.min()));
		json.add("max", writeVec3(bounds.max()));
		return json;
	}

	private static BoundingBox readBounds(JsonObject json) {
		return new BoundingBox(readVec3(json.getAsJsonObject("min")), readVec3(json.getAsJsonObject("max")));
	}

	private static JsonObject writeVec3(Vec3d vector) {
		JsonObject json = new JsonObject();
		json.addProperty("x", vector.x());
		json.addProperty("y", vector.y());
		json.addProperty("z", vector.z());
		return json;
	}

	private static Vec3d readVec3(JsonObject json) {
		return new Vec3d(json.get("x").getAsDouble(), json.get("y").getAsDouble(), json.get("z").getAsDouble());
	}
}
