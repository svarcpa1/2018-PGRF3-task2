#version 150

in vec2 inPosition;
in vec2 inTexture;

uniform mat4 viewMat;
uniform mat4 projMat;
uniform mat4 MVPMatLight;
uniform float time;
uniform int modeOfFunction, modeOfLight, modeOfSurface;

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
    float y = 2*sin(azimuth)*sin(zenith);
    float z = cos(zenith)*0.1*time;

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
//mine cylin
vec3 getSlider(vec2 xy){
	float s=  PI * 0.5 - PI * xy.x;
	float t= (PI * 0.5 - PI * xy.y);

    float theta = t;
	float r = (1+sin(s)+cos(t))*2;
	float z =t+3;

	return vec3(r*cos(theta), r*sin(theta),z)/2;
}
//cylin
vec3 getGoblet(vec2 xy){
	float s =  PI * 0.5 - PI * xy.x;
	float t = (PI * 0.5 - PI * xy.y)/1.5;

    float theta = s;
	float r = 1+cos(t);
	float z =t;

	return vec3(r*cos(theta), r*sin(theta),z)/2;
}
// mine spheric
vec3 getSomething(vec2 xy){
    float s = xy.x * PI;
    float t = xy.y * PI;
    float r = sin(t-PI);

    float x = r*sin(t)*cos(s);
    float y = r*sin(t)*sin(s);
    float z = r*cos(t);

    return vec3(x, y, z);
}
//spheric
vec3 getElephant(vec2 xy){
    float s = xy.x * PI;
    float t = xy.y * PI;
    float r = 3+cos(4*s);

    float x = r*sin(t)*cos(s);
    float y = r*sin(t)*sin(s);
    float z = r*cos(t);

    return vec3(x, y, z)*0.25;
}

// normals counted by differention
vec3 getNormalDiff(vec2 xy){
    vec3 u;
    vec3 v;

    if(modeOfFunction==0){
        u = getTrampoline(xy + vec2(0.001,0)) - getTrampoline(xy - vec2(0.001,0));
        v = getTrampoline(xy + vec2(0, 0.001)) - getTrampoline(xy - vec2(0, 0.001));
    }else if(modeOfFunction==1){
        u = getSlider(xy + vec2(0.001,0)) - getSlider(xy - vec2(0.001,0));
        v = getSlider(xy + vec2(0, 0.001)) - getSlider(xy - vec2(0, 0.001));
    }else if(modeOfFunction==2){
        u = getGoblet(xy + vec2(0.001,0)) - getGoblet(xy - vec2(0.001,0));
        v = getGoblet(xy + vec2(0, 0.001)) - getGoblet(xy - vec2(0, 0.001));
    }else if(modeOfFunction==3){
        u = getElephant(xy + vec2(0.001,0)) - getElephant(xy - vec2(0.001,0));
        v = getElephant(xy + vec2(0, 0.001)) - getElephant(xy - vec2(0, 0.001));
    }else if(modeOfFunction==4){
        u = getSomething(xy + vec2(0.001,0)) - getSomething(xy - vec2(0.001,0));
        v = getSomething(xy + vec2(0, 0.001)) - getSomething(xy - vec2(0, 0.001));
    }
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

    //generate still plain
    if(modeOfFunction == 11){
        pos4=vec4(pos*3, 2.0, 1.0);
        normal=vec3(pos,2.0);
        //for moving light with us
        //normal = inverse(transpose(mat3(viewMat))) * normal;
    }
    //generate moving sphere
    if(modeOfFunction == 10){
        pos4 = vec4(getSphere(pos)/3, 1.0);
        pos4 = vec4(pos4.x+1.8, pos4.y+0+time/10, (pos4.z+3), pos4.w);
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
    if(modeOfFunction == 1){
        pos4 = vec4(getSlider(pos), 1.0);
        pos4 = vec4(pos4.xy, pos4.z+1, pos4.w);
        normal= getNormalDiff(pos);
    }
    if(modeOfFunction == 2){
        pos4 = vec4(getGoblet(pos), 1.0);
        pos4 = vec4(pos4.xy, pos4.z +3.5, pos4.w);
        normal= getNormalDiff(pos);
    }
    if(modeOfFunction == 3){
        pos4 = vec4(getElephant(pos), 1.0);
        pos4 = vec4(pos4.xy, pos4.z +3.5, pos4.w);
        normal= getNormalDiff(pos);
    }
    if(modeOfFunction == 4){
        pos4 = vec4(getSomething(pos), 1.0);
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

	//per vertec mode
	if(modeOfLight==0){
        //depth texture
        textCoordinatesDepth = MVPMatLight*pos4;

        //color
        if(modeOfSurface==0){
            vertColor = vec3(dot(normalize(normal), normalize(light)))*vec3(0.8,0.1,0.1);
        }
        //texture
        else if(modeOfSurface==1){
            vertColor = pos4.xyz;
            vertColor = vec3(dot(normalize(normal), normalize(light)));
            //texture
            textCoordinates=inTexture;
        }
        //normal
        else{
            vertColor = vec3(dot(normalize(normal), normalize(light)));
            vertColor = vertColor * normalize(normal);
        }

	//per pixel mode
	}else{
        //textures
        if(modeOfSurface==1){
            textCoordinates=inTexture;
        }else{
            textCoordinates=vec2(0,0);
        }

        textCoordinatesDepth = MVPMatLight*pos4;
    }
}
