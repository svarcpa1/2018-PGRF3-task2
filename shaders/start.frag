#version 150

in vec3 vertColor;
in vec2 textCoordinates;
in vec4 textCoordinatesDepth;
in vec3 normal;
in vec3 light;
in vec3 viewDirection;
in vec2 inPosition2;

uniform sampler2D textureSampler;
uniform sampler2D textureSamplerDepth;
uniform sampler2D textureSamplerNrm;
uniform sampler2D textureSamplerDisp;
uniform int modeOfLight, modeOfLightSource, modeOfMapping;
uniform float time;

out vec4 outColor; // output from the fragment shader

void main() {
    vec4 baseColor = texture(textureSampler, textCoordinates);
    vec2 textC = textCoordinates;

	//per vertex mode
	if(modeOfLight==0){
        outColor = texture2D(textureSampler, textCoordinates);
        outColor = outColor * vec4(vertColor, 1.0);

        vec3 textCoordinatesDepthTmp;
        textCoordinatesDepthTmp = (textCoordinatesDepth.xyz/textCoordinatesDepth.w + 1.)/2.;
        if (texture(textureSamplerDepth, textCoordinatesDepthTmp.xy).z < textCoordinatesDepthTmp.z-0.0005){
            outColor = outColor*0.5;
        }else{
            outColor = outColor;
        }
	}

    //per pixel mode
	else{
        vec3 norm = normal;
        vec3 ld = normalize( light );
        vec3 vd = normalize( viewDirection );

        //if paralax
        if(modeOfMapping==1){
            float height = texture2D(textureSamplerDisp, inPosition2).r;
            float scaleL=0.04;
            float scaleH=0.0;
            //textC = inPosition2 + (ld.xy * (height * scaleL - scaleH)).yx;
            textC = inPosition2 + light.xy/light.z*(height * scaleL - scaleH);

            //culling
            if(textC.x> 1.0 || textC.y> 1.0 || textC.x< 0.0 || textC.y< 0.0) discard;
        }

        //normal mapping
        norm = texture2D(textureSamplerNrm, textC).xyz * 2.0 - 1.0;
        vec3 nd = normalize( norm );

        vec4 ambient = vec4(0.3,0.3,0.3,1);
        vec4 diffuse = vec4(0.5,0.5,0.5,1);
        vec4 specular = vec4(0.9,0.9,0.0,1);
        vec4 totalAmbient = ambient * baseColor;
        vec4 totalDiffuse = vec4(0.0);
        vec4 totalSpecular = vec4(0.0);

        float NDotL = max(dot( nd, ld), 0.0 );
        vec3 reflection = normalize(((2.0 * nd)*NDotL)-ld);
        float RDotV = max(0.0, dot(reflection, vd));
        vec3 halfVector = normalize( ld + vd);
        float NDotH = max(0.0, dot(nd, halfVector));

        totalDiffuse = diffuse * NDotL * baseColor;
        totalSpecular = specular * (pow(NDotH, 16));

        //spolight + testing if a object is in light or not
        float spotOff = cos(radians(30));
        if(modeOfLightSource==1){
            float spotEffect = max(dot(normalize(vec3(0,0,0)-vec3(5,0+time/3,8)),normalize(-ld)),0);
            float blend = clamp((spotEffect-spotOff)/(1-spotOff) ,0.0,1.0);

            if(spotEffect>spotOff){
                outColor = mix(totalAmbient,(totalAmbient + (totalDiffuse + totalSpecular)),blend);
            }else{
                outColor=totalAmbient;
            }

        }else{
            outColor = (totalAmbient + (totalDiffuse + totalSpecular));
        }

        vec3 textCoordinatesDepthTmp;
        textCoordinatesDepthTmp = (textCoordinatesDepth.xyz/textCoordinatesDepth.w + 1.)/2.;

        vec4 outColor2 = texture2D(textureSampler, textCoordinates);

        if ((texture(textureSamplerDepth, textCoordinatesDepthTmp.xy).z < textCoordinatesDepthTmp.z-0.0005)){
            outColor=outColor2*totalAmbient;
        }else{
            outColor=outColor2*outColor;
        }
    }
} 
