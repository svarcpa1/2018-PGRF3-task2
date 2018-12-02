#version 150

in vec2 inPosition;
in vec2 inTexture;

uniform mat4 viewMat;
uniform mat4 projMat;
uniform mat4 MVPMatLight;
uniform float time;
uniform int modeOfFunction, modeOfLight;

out vec3 vertColor;
out vec2 textCoordinates;
out vec4 textCoordinatesDepth;
out vec3 normal;
out vec3 light;
out vec3 viewDirection;

float PI = 3.1415;

float functionForZ(vec2 vec){
    return sin (vec.x * 2 * 3.14 + time);
}

//mine kart
vec3 getTrampoline(vec2 xy){
    float zenith = xy.x * PI;
    float azimuth = xy.y * PI;
    float r = 2 + sin(zenith + azimuth);

    float x = sin(zenith)*cos(azimuth);
    float y = sin(azimuth)*sin(zenith);
    float z = cos(zenith)*0.1*5;

    return vec3(x, y, z);
}
//kart
vec3 getSphere(vec2 xy){
    //0-2pi
    float azimuth = xy.x * PI;
    //0-pi
    float zenith = xy.y * PI/2;
    float r = 1;

    float x = cos(azimuth)*cos(zenith)*r;
    float y = sin(azimuth)*cos(zenith)*r;
    float z = sin(zenith)*r;

    return vec3(x, y, z);
}


// normals counted by differention for trampoline
vec3 getNormalDiff(vec2 xy){
    vec3 u;
    vec3 v;

    u = getTrampoline(xy + vec2(0.001,0)) - getTrampoline(xy - vec2(0.001,0));
    v = getTrampoline(xy + vec2(0, 0.001)) - getTrampoline(xy - vec2(0, 0.001));

    return cross(u,v);
}

//normlas counted by parcial derivation (sphere)
vec3 getSphereNormal(vec2 xy){
    float az = xy.x * PI;
    float ze = xy.y * PI/2;
    float r = 1;

    vec3 dx = vec3(-sin(az)*cos(ze)*PI, cos(az)*cos(ze)*PI, 0);
    vec3 dy = vec3(cos(az)*-sin(ze)*PI/2, sin(az)*-sin(ze)*PI/2, cos(ze)*PI/2);
    return cross(dx,dy);
}

void main() {
    vec2 pos = inPosition*2 - 1;
    vec4 pos4;
    normal;

/*    //generate still plain
    if(modeOfFunction == 11){
        pos4=vec4(pos*3, 2.0, 1.0);
        normal=vec3(pos,2.0);
        //for moving light with us
        //normal = inverse(transpose(mat3(viewMat))) * normal;
    }*/

    //generate moving sphere
    if(modeOfFunction == 10){
        pos4 = vec4(getSphere(pos)/3, 1.0);
        pos4 = vec4(pos4.x+1.8, pos4.y, (pos4.z+3), pos4.w);
        normal= getSphereNormal(pos);
    }
    //generate "sun"
    if(modeOfFunction == 12){
        pos4 = vec4(getSphere(pos)/8, 1.0);
        pos4 = vec4(pos4.x+5, pos4.y+time/3, pos4.z+8, pos4.w);
        normal= getSphereNormal(pos);
    }
    if(modeOfFunction == 0){
        pos4 = vec4(getTrampoline(pos), 1.0);
        pos4 = vec4(pos4.xy, pos4.z +3.5, pos4.w);
        normal= getNormalDiff(pos);
    }

    viewDirection = -(viewMat*pos4).xyz;
	gl_Position = projMat * viewMat * pos4;

	//light
	vec3 lightPos = vec3(5, 0+time/3, 8);

	//this would be used for moving light with viewer
	//light = lightPos-(viewMat*pos4).xyz;
	light = lightPos-(pos4).xyz;

	//per vertex mode
	if(modeOfLight==0){
        //depth texture
        textCoordinatesDepth = MVPMatLight*pos4;

        vertColor = pos4.xyz;
        vertColor = vec3(dot(normalize(normal), normalize(light)));
        //texture
        textCoordinates=inTexture;

	//per pixel mode
	}else{
        textCoordinates=inTexture;
        textCoordinatesDepth = MVPMatLight*pos4;
    }
}
