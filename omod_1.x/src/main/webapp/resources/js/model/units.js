define(
    [
        openhmis.url.backboneBase + 'js/lib/backbone',
        openhmis.url.backboneBase + 'js/model/generic'
    ],
    //162384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    //http://208.77.196.178:10080/openmrs/ws/rest/v1/concept
    //http://208.77.196.178:10080/openmrs/ws/rest/v1/concept/162384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    function(Backbone, openhmis) {
    	openhmis.Units = openhmis.GenericModel.extend({
    		meta: {
    			name: openhmis.getMessage('openhmis.backboneforms.concept.name'),
                namePlural: openhmis.getMessage('openhmis.backboneforms.concept.namePlural'),
                restUrl: 'v1/concept/162384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
    		},

    		parse: function(response) {
            	return response;
            },

    		schema: {
    			display: { type: 'Text' },
    		},

    		toString: function() {
    			return this.get('display');
			}
    	});

    	openhmis.UnitsCollection = openhmis.GenericCollection.extend({
        	model: openhmis.Units,


        	parse: function(response) {
        		var result = response.setMembers;
        		result.unshift({display:''});
        		return result;
        	}

        });

    	return openhmis;
    }
);