/*
 *  Copyright 2013-2018 - Monentia
 *
 *   Daccosim is a collaborative development effort between EDF (France),
 *   CentraleSupélec (France), EIFER Institute (Germany), SIANI institute (Spain)
 *   and Monentia S.L. (Spain)
 *
 *  This File is part of Daccosim Project
 *
 * Daccosim Project is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License.
 *
 *  Daccosim Project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Daccosim. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.simulage.daccosim.view;

import com.google.gson.*;
import eu.simulage.daccosim.DataType;

import java.lang.reflect.Type;

import static eu.simulage.daccosim.view.Helper.defaultValueOf;

public class Adapters {
	private static Object valueOf(JsonObject object) {
		if (!object.has("value")) return defaultValueOf(DataType.valueOf(object.get("type").getAsString()));
		JsonPrimitive primitive = object.get("value").getAsJsonPrimitive();
		if (primitive.isBoolean()) return primitive.getAsBoolean();
		if (primitive.isString()) return primitive.getAsString();
		if (primitive.toString().contains(".")) return primitive.getAsDouble();
		return primitive.getAsInt();
	}

	public static class GraphNodeAdapter implements JsonSerializer<GraphNode>, JsonDeserializer<GraphNode> {

		private static final String CLASSNAME = "CLASSNAME";
		private static final String INSTANCE = "INSTANCE";

		@Override
		public JsonElement serialize(GraphNode src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject retValue = new JsonObject();
			String className = src.getClass().getName();
			retValue.addProperty(CLASSNAME, className);
			JsonElement elem = context.serialize(src);
			retValue.add(INSTANCE, elem);
			return retValue;
		}

		@Override
		public GraphNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
			String className = prim.getAsString();

			Class<?> klass;
			try {
				klass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new JsonParseException(e.getMessage());
			}
			JsonObject instance = jsonObject.get(INSTANCE).getAsJsonObject();
			GraphNode deserialize = context.deserialize(instance, klass);
			if (instance.has("value"))
				((Operator) deserialize).value(valueOf(instance));
			return deserialize;
		}


	}

	static class InitialValueJsonDeserializer implements JsonDeserializer<InitialValue> {
		@Override
		public InitialValue deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
			JsonObject object = json.getAsJsonObject();
			return new InitialValue(
					object.get("name").getAsString(),
					valueOf(object));
		}
	}

	static class InputJsonDeserializer implements JsonDeserializer<Input> {
		@Override
		public Input deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
			JsonObject object = json.getAsJsonObject();
			return Helper.input(
					object.get("id").getAsString(),
					DataType.valueOf(object.get("type").getAsString()),
					valueOf(object),
					object.get("variability") != null ? object.get("variability").getAsString() : "discrete");
		}
	}

	static class OutputJsonDeserializer implements JsonDeserializer<Output> {
		@Override
		public Output deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
			JsonObject object = json.getAsJsonObject();
			return Helper.output(
					object.get("id").getAsString(),
					DataType.valueOf(object.get("type").getAsString()),
					object.get("variability") != null ? object.get("variability").getAsString() : "discrete", valueOf(object)
			);
		}
	}
}
